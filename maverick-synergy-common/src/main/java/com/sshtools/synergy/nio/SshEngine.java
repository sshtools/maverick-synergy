/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.synergy.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.net.HttpRequest;
import com.sshtools.common.net.HttpResponse;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.Utils;
import com.sshtools.synergy.ssh.Connection;

/**
 * This class provides an abstract daemon for servicing any number of protocol
 * contexts.
 */
public class SshEngine {
	
	static SshEngine defaultInstance = null;
	
	SshEngineContext context;
	SelectorThreadPool acceptThreads;
	SelectorThreadPool connectThreads;
	SelectorThreadPool transferThreads;
	Map<String,ProtocolClientAcceptor> acceptors = new ConcurrentHashMap<String,ProtocolClientAcceptor>(50, 0.9f, 1);
	Thread shutdownHook;
	boolean started;
	boolean isStarting = false;
	boolean startupRequiresListeningInterfaces = false;
	List<ListeningInterface> listeningInterfaces = Collections.synchronizedList(new ArrayList<ListeningInterface>());
	ConcurrentLinkedQueue<Runnable> shutdownHooks = new ConcurrentLinkedQueue<Runnable>();
	Throwable lastError = null;
	AbstractRequestFuture shutdownFuture = new ChannelRequestFuture();
	Object lock = new Object();

	List<SshEngineListener> listeners = new CopyOnWriteArrayList<SshEngineListener>();
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	private final static String[] SOCKSV5_ERROR = {
	        "Success", "General SOCKS server failure",
	        "Connection not allowed by ruleset", "Network unreachable",
	        "Host unreachable", "Connection refused", "TTL expired",
	        "Command not supported", "Address type not supported"
	    };

    private final static String[] SOCKSV4_ERROR = {
        "Request rejected or failed",
        "SOCKS server cannot connect to identd on the client",
        "The client program and identd report different user-ids"
    };

    private static final int SOCKS4 = 0x04;
    private static final int SOCKS5 = 0x05;
    private static final int CONNECT = 0x01;
    private static final int NULL_TERMINATION = 0x00;
    
	/**
	 * Constructs the Daemon.
	 */
	public SshEngine() {
		context = new SshEngineContext(this);
	}

	/**
	 * Get the context for this Daemon.
	 * 
	 * @return DaemonContext
	 */
	public SshEngineContext getContext() {
		return context;
	}

	private static String version = PomVersion.getVersion();
	
	public Throwable getLastError() {
		return lastError;
	}
	
	
	public void addListener(SshEngineListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(SshEngineListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Returns the current version of the API.
	 * 
	 * @returns the current version in the form MAJOR.MINOR.BUILD
	 */
	public static String getVersion() {
		return version;
	}

	/**
	 * Returns the release date of the current version.
	 * 
	 * @returns the release date of the current version.
	 */
	public static Date getReleaseDate() {
		return new Date(/* RELEASE_DATE */);
	}

	public boolean isStarting() {
		return isStarting;
	}
	
	public void addShutdownHook(Runnable r) {
		shutdownHooks.add(r);
	}

	protected int getIntValue(Properties properties, String overrideKey, int defaultValue) {
		if(properties.containsKey(overrideKey)) {
			try {
				return Integer.parseInt(properties.getProperty(overrideKey));
			} catch (NumberFormatException e) {
			}
		}
		return defaultValue;
	}
	
	protected boolean getBooleanValue(Properties properties, String overrideKey, boolean defaultValue) {
		if(properties.containsKey(overrideKey)) {
			try {
				return Boolean.parseBoolean(properties.getProperty(overrideKey));
			} catch (NumberFormatException e) {
			}
		}
		return defaultValue;
	}
	
	protected long getLongValue(Properties properties, String overrideKey, long defaultValue) {
		if(properties.containsKey(overrideKey)) {
			try {
				return Long.parseLong(properties.getProperty(overrideKey));
			} catch (NumberFormatException e) {
			}
		}
		return defaultValue;
	}
	
	/**
	 * Starts the daemon, first calling the
	 * {@link #configure(ConfigurationContext)} method to allow your server to
	 * configure itself.
	 * 
	 * @throws IOException
	 * @return <tt>true</tt> if at least one interface started, otherwise
	 *         <tt>false</tt>.
	 */
	public boolean startup() throws IOException {
		synchronized(lock) {
			return startup(System.getProperties());
		}
	}
	
	public boolean startup(Properties properties) throws IOException {

		synchronized(lock) {
			isStarting = true;
			lastError = null;
			try {

				for(SshEngineListener listener : listeners) {
					listener.starting(this);
				}
				
				shutdownHook = new Thread() {
					public void run() {
						if(Log.isInfoEnabled())
							Log.info("The system is shutting down");
						shutdownNow(false, getLongValue(properties, 
								"maverick.config.shutdown.defaultGracePeriod", 5000L));
					}
				};
	
				if(Log.isInfoEnabled()) {
	
					Log.info("Product version: " + version);
					Log.info("Java version: "
							+ System.getProperty("java.version"));
	
					Log.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
	
	
					Log.info("Configuring SSH engine");
				}
	
				if(Log.isInfoEnabled())
					Log.info("Configuration complete");
	
				if (Runtime.getRuntime() != null)
					Runtime.getRuntime().addShutdownHook(shutdownHook);
	
				connectThreads = new SelectorThreadPool(
						new ConnectSelectorThread(),
						getIntValue(properties, "maverick.config.connect.threads", context.getPermanentConnectThreads()),
						getIntValue(properties, "maverick.config.channelsPerThread", context.getMaximumChannelsPerThread()),
						getIntValue(properties, "maverick.config.idlePeriod", context.getIdleServiceRunPeriod()),
						getIntValue(properties, "maverick.config.idleEvents", context.getInactiveServiceRunsPerIdleEvent()),
						context.getSelectorProvider());
	
				transferThreads = new SelectorThreadPool(
						new TransferSelectorThread(),
						getIntValue(properties, "maverick.config.transfer.threads", context.getPermanentTransferThreads()),
						getIntValue(properties, "maverick.config.channelsPerThread", context.getMaximumChannelsPerThread()),
						getIntValue(properties, "maverick.config.idlePeriod", context.getIdleServiceRunPeriod()),
						getIntValue(properties, "maverick.config.idleEvents", context.getInactiveServiceRunsPerIdleEvent()),
						context.getSelectorProvider());
	
				acceptThreads = new SelectorThreadPool(new AcceptSelectorThread(),
						getIntValue(properties, "maverick.config.accept.threads", context.getPermanentAcceptThreads()),
						getIntValue(properties, "maverick.config.channelsPerThread", context.getMaximumChannelsPerThread()),
						getIntValue(properties, "maverick.config.idlePeriod", context.getIdleServiceRunPeriod()),
						getIntValue(properties, "maverick.config.idleEvents", context.getInactiveServiceRunsPerIdleEvent()),
						context.getSelectorProvider());
	
				ListeningInterface[] interfaces = context.getListeningInterfaces();
	
				int listening = 0;
				for (int i = 0; i < interfaces.length; i++) {
					if (startListeningInterface(interfaces[i]))
						listening++;
				}
	
				if (listening == 0 && startupRequiresListeningInterfaces) {
					if(Log.isInfoEnabled())
						Log.info("No listening interfaces were bound!");
					shutdownNow(false, 0);
					return false;
				}
	
				started = true;
				
				for(SshEngineListener listener : listeners) {
					listener.started(this);
				}
				
				if(getBooleanValue(properties, "maverick.threadDump", false)) {
					new Thread("ThreadMonitor") {
						public void run() {
							while(isStarted()) {
								
								try {
									Thread.sleep(getLongValue(properties, "maverick.threadDumpInterval", 300000L));
								} catch (InterruptedException e) {
								}
								
								Log.raw(Level.INFO, Utils.generateThreadDump(), true);
							}
						}
					}.start();
				}
				return true;
	
			} catch (Throwable ex) {
				if(Log.isInfoEnabled())
					Log.info("The engine failed to start", ex);
				lastError = ex;
				shutdownNow(false, 0);
				if (ex instanceof LicenseException)
					throw (IOException) ex;
				return false;
			} finally {
				isStarting = false;
			}
		}

	}

	protected boolean startListeningInterface(ListeningInterface li) {

		if(Log.isInfoEnabled())
			Log.info("Binding server to "
					+ li.getAddressToBind().toString());

		try {
			
			ServerSocketChannel socketChannel = context.getSelectorProvider()
					.openServerSocketChannel();
			socketChannel.configureBlocking(false);
			
			socketChannel.socket().setReuseAddress(
					li.getSocketOptionReuseAddress());

			ServerSocket socket = socketChannel.socket();
			
			socket.bind(li.getAddressToBind(), li.getBacklog());
			
			li.setActualPort(socket.getLocalPort());
			socket.setReuseAddress(li.getSocketOptionReuseAddress());

			ProtocolClientAcceptor a = new ProtocolClientAcceptor(li,
					socketChannel);

			registerAcceptor(a, socketChannel);

			acceptors.put(li.getAddressToBind().toString(), a);

			for(SshEngineListener listener : listeners) {
				listener.interfaceStarted(this, li);
			}
			
			listeningInterfaces.add(li);
			
			
			return true;

		} catch (IOException ex) {
			if(Log.isInfoEnabled())
				Log.info("Failed to bind to "
						+ li.getAddressToBind().toString(), ex);
			try {
				context.removeListeningInterface(li.getAddressToBind().getAddress().getHostAddress(), li.getActualPort());
			} catch (UnknownHostException e) {
			}
			lastError = ex;
			for(SshEngineListener listener : listeners) {
				listener.interfaceCannotStart(this, li, ex);
			}
			return false;
		}
		
	}

	public void removeAcceptor(ListeningInterface li) {

		if(Log.isInfoEnabled())
			Log.info("Removing interface " + li.getAddressToBind().toString());

		ProtocolClientAcceptor a = acceptors.remove(li
				.getAddressToBind().toString());
		
		try {
			if (a != null) {
				a.stopAccepting();
			}
			
			listeningInterfaces.remove(li);
			
			for(SshEngineListener listener : listeners) {
				listener.interfaceStopped(this, li);
			}
		
		} catch(IOException ex) {
			for(SshEngineListener listener : listeners) {
				listener.interfaceCannotStop(this, li, ex);
			}
		}
	}

	/**
	 * Get whether the daemon is currently started
	 * 
	 * @return started
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * Shutdown the server. This does not exit the JVM
	 */
	public void shutdownAsync(final boolean graceful, final long forceAfterMs) {
		Thread t = new Thread() {
			public void run() {
				shutdownNow(graceful, forceAfterMs);
			}
		};
		t.start();
	}
	
	/**
	 * This method should be used to shutdown the server from your main thread. If you need to shutdown
	 * the server from within a session that is running on a transfer thread use {@link shutdownAsync().}
	 */
	public void shutdownNow(boolean graceful, long forceAfterMs) {
	
		synchronized(lock) {
			try {
	
				for(SshEngineListener listener : listeners) {
					listener.shuttingDown(this);
				}
				
				// Stop accepting new connections
				if (acceptThreads != null)
					acceptThreads.shutdown();
				
				for(ListeningInterface li : listeningInterfaces) {
					for(SshEngineListener listener : listeners) {
						listener.interfaceStopped(this, li);
					}
				}
				
				listeningInterfaces.clear();
				
				if(graceful) {
				
					long started = System.currentTimeMillis();
	
					while (transferThreads.getCurrentLoad() > 0) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
	
						if (forceAfterMs > 0
								&& System.currentTimeMillis() - started > forceAfterMs)
							break;
					}
				}
				
				// First close the channels whilst maintaining I/O
				if(transferThreads!=null) {
					transferThreads.closeAllChannels();
				}
				
				try {
					if (Runtime.getRuntime() != null && shutdownHook!=null)
						Runtime.getRuntime().removeShutdownHook(shutdownHook);
				} catch (IllegalStateException ex) {
				} finally {
					shutdownHook = null;
					
					for(SshEngineListener listener : listeners) {
						listener.shutdown(this);
					}
				}
				
				// Run any shutdown hooks
				if(shutdownHooks!=null) {
					for(Runnable r : shutdownHooks) {
						try {
							r.run();
						} catch (Exception e) {
						}
					}
				}
	
				if (connectThreads != null)
					connectThreads.shutdown();
				
				if (transferThreads != null)
					transferThreads.shutdown();
	
				
			} finally {
				started = false;
				shutdownFuture.done(true);
			}
		}
	}
	
	public void shutdownAndExit() {
		shutdownNow(false, 0L);
		Log.getDefaultContext().shutdown();
	}

	public void restart() throws IOException {
		restart(false, 0);
	}

	public void restart(boolean graceful, long forceAfterMs) throws IOException {

		shutdownNow(graceful, forceAfterMs);
		startup();

	}
	
    public <K extends ProtocolContext> ConnectRequestFuture connect(String hostToConnect, int portToConnect, K protocolContext) throws SshException, IOException {
		
    	SocketChannel socketChannel = SocketChannel.open();
		
	    socketChannel.configureBlocking(true);
	    socketChannel.socket().setTcpNoDelay(true);

	    final ConnectRequestFuture future;
	    boolean connected;
	    
	    switch(protocolContext.getProxyType()) {
	    case NONE:
	    	future = new ConnectRequestFuture(hostToConnect, portToConnect);
	    	connected = socketChannel.connect(new InetSocketAddress(hostToConnect, portToConnect));
	    	break;
	    default:
	    	future = new ConnectRequestFuture(protocolContext.getProxyHostname(), protocolContext.getProxyPort());
	    	connected = socketChannel.connect(new InetSocketAddress(protocolContext.getProxyHostname(), protocolContext.getProxyPort()));
	    }
	     
	    if(connected) {
	       processOpenSocket(socketChannel, protocolContext, hostToConnect, portToConnect);
	       socketChannel.configureBlocking(false);
	       registerClientConnection(protocolContext, socketChannel, future);
	       
	    } else {
		
		    // Register the connector and we will confirm once we have connected
			registerConnector(new DaemonClientConnector(protocolContext, socketChannel, future, hostToConnect, portToConnect), socketChannel);
	    }

	    return future;
    }

    private void sendHTTPProxyRequest(SocketChannel channel, ProtocolContext protocolContext, String hostToConnect, int portToConnect) throws UnsupportedEncodingException, IOException {
		
		if(Log.isDebugEnabled()) {
			Log.debug("Connecting via HTTP proxy {}:{}", protocolContext.getProxyHostname(), protocolContext.getProxyPort());
		}
		
		HttpRequest request = new HttpRequest();

        request.setHeaderBegin("CONNECT " 
        			+ hostToConnect 
        			+ ":" 
        			+ portToConnect
        			+ " HTTP/1.0");
        request.setHeaderField("User-Agent", Utils.defaultString(protocolContext.getUserAgent(), "MaverickSynergy/" + version));
        request.setHeaderField("Pragma", "No-Cache");
        request.setHeaderField("Host", protocolContext.getProxyHostname());
        request.setHeaderField("Proxy-Connection", "Keep-Alive");
        
        if(Utils.isNotBlank(protocolContext.getProxyUsername()) && !Utils.isNotBlank(protocolContext.getProxyPassword())) {
        	request.setBasicAuthentication(protocolContext.getProxyUsername(), protocolContext.getProxyPassword());
        }
        

		channel.write(ByteBuffer.wrap(request.toString().getBytes("UTF-8")));
		
		
		ByteBuffer buf = ByteBuffer.allocate(4096);
		int count;
		do {
			count = channel.read(buf);
		} while(count > -1 && !isHTTPResponseComplete(buf));
		
		HttpResponse resp = new HttpResponse();
		buf.flip();
		
		resp.process(buf);

		if(resp.getStatus()!=200) {
			throw new IOException("Invalid HTTP proxy response! " + resp.getStartLine());
		}

	}

	private boolean isHTTPResponseComplete(ByteBuffer buf) {
		buf.flip();
		boolean eol = false;
		if(buf.remaining() > 4) {
			eol = buf.get(buf.remaining()-4) == '\r'
					&& buf.get(buf.remaining()-3) == '\n'
					&& buf.get(buf.remaining()-2) == '\r'
					&& buf.get(buf.remaining()-1) == '\n';
		}
		buf.compact();
		return eol;
	}

	protected SocketChannel processOpenSocket(SocketChannel socketChannel, ProtocolContext protocolContext,
			String hostToConnect, int portToConnect) throws UnsupportedEncodingException, IOException {
		
		switch(protocolContext.getProxyType()) {
	    case HTTP:
	    	sendHTTPProxyRequest(socketChannel, protocolContext, hostToConnect, portToConnect);
	    	break;
	    case SOCKS4:
	    	sendSOCKS4ProxyRequest(socketChannel, protocolContext, hostToConnect, portToConnect);
	    	break;
	    case SOCKS5:
	    	sendSOCKS5ProxyRequest(socketChannel, protocolContext, hostToConnect, portToConnect);
	    	break;
	    default:
	    	break;
	    }
		return socketChannel;
	}
	
	private int readByte(SocketChannel channel) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(1);
		int r = channel.read(b);
		if(r == -1) {
			throw new IOException("Socket disconnected whilst expecting data");
		}
		if(r == 0) {
			throw new IOException("Unexecpted zero bytes returned from socket");
		}
		return b.get(0) & 0xFF;
	}
	
	private void sendSOCKS4ProxyRequest(SocketChannel socketChannel, ProtocolContext protocolContext,
			String hostToConnect, int portToConnect) throws IOException {
		
		if(Log.isDebugEnabled()) {
			Log.debug("Connecting via SOCKS4 proxy {}:{}", protocolContext.getProxyHostname(), protocolContext.getProxyPort());
		}
		
		InetAddress hostAddr = InetAddress.getByName(hostToConnect);
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		buf.put((byte)SOCKS4);
		buf.put((byte)CONNECT);
		buf.put((byte)((portToConnect >>> 8) & 0xff));
		buf.put((byte)(portToConnect & 0xff));
		buf.put(hostAddr.getAddress());
		buf.put(protocolContext.getProxyUsername().getBytes("UTF-8"));
		buf.put((byte)NULL_TERMINATION);

		buf.flip();
		
		socketChannel.write(buf);
		
		int res = readByte(socketChannel);

        if (res != 0x00) {
            throw new IOException("Invalid response from SOCKS4 server (" +
                res + ") " + protocolContext.getProxyHostname() + ":" + protocolContext.getProxyPort());
        }
        
        int code = readByte(socketChannel);

        if (code != 90) {
            if ((code > 90) && (code < 93)) {
                throw new IOException(
                    "SOCKS4 server unable to connect, reason: " +
                    SOCKSV4_ERROR[code - 91]);
            }
			throw new IOException(
			    "SOCKS4 server unable to connect, reason: " + code);
        }

        ByteBuffer data = ByteBuffer.allocate(6);
        
        int c = 0;
        while(c < 6) {
        	int r = socketChannel.read(data);
        	if(r > 0) {
        		c+= r;
        	}
        }

	}

	private void sendSOCKS5ProxyRequest(SocketChannel socketChannel, ProtocolContext protocolContext,
			String hostToConnect, int portToConnect) throws IOException {
		
		if(Log.isDebugEnabled()) {
			Log.debug("Connecting via SOCKS5 proxy {}:{}", protocolContext.getProxyHostname(), protocolContext.getProxyPort());
		}
		
		byte[] request = {
                (byte) SOCKS5, (byte) 0x02, (byte) 0x00, (byte) 0x02
        };
		
        socketChannel.write(ByteBuffer.wrap(request));
        
        int res = readByte(socketChannel);

        if (res != 0x05) {
            throw new IOException("Invalid response from SOCKS5 server (" +
                res + ") " + protocolContext.getProxyHostname() + ":" +
                protocolContext.getProxyPort());
        }
        
        int method = readByte(socketChannel);

        switch (method) {
        case 0x00:
            break;

        case 0x02:

        	try(ByteArrayWriter baw  = new ByteArrayWriter()) {
        		baw.write(0x01);
        		
        		byte[] h = protocolContext.getProxyUsername().getBytes("UTF-8");
	            baw.write(h.length);
	            baw.write(h);
	            
	            h = protocolContext.getProxyPassword().getBytes("UTF-8");
	            baw.write(h.length);
	            baw.write(h);

	            socketChannel.write(ByteBuffer.wrap(baw.toByteArray()));
        	}
            
            res = readByte(socketChannel);

            if ((res != 0x01) && (res != 0x05)) {
                throw new IOException("Invalid response from SOCKS5 server (" +
                    res + ") " + protocolContext.getProxyHostname() + ":" + protocolContext.getProxyPort());
            }

            if (readByte(socketChannel) != 0x00) {
                throw new IOException("Invalid username/password for SOCKS5 server");
            }
            break;

        default:
            throw new IOException(
                "SOCKS5 server does not support our authentication methods");
        }

        try(ByteArrayWriter baw = new ByteArrayWriter()) {
	        if (protocolContext.isResolveLocally()) {
	            InetAddress hostAddr;
	
	            try {
	                hostAddr = InetAddress.getByName(hostToConnect);
	            } catch (UnknownHostException e) {
	                throw new IOException("Can't do local lookup on: " +
	                		hostToConnect + ", try socks5 without local lookup");
	            }
	
	            request = new byte[] {
	                    (byte) SOCKS5, (byte) 0x01, (byte) 0x00, (byte) 0x01
	            };
	
	            baw.write(request);
	            baw.write(hostAddr.getAddress());
	        } else {
	            request = new byte[] {
	                (byte) SOCKS5, (byte) 0x01, (byte) 0x00, (byte) 0x03
	            };
	            
	            baw.write(request);
	            byte[] h = hostToConnect.getBytes("UTF-8");
	            baw.write(h.length);
	            baw.write(h);
	            
	        }
	
	        baw.write((portToConnect >>> 8) & 0xff);
	        baw.write(portToConnect & 0xff);
	        
	        socketChannel.write(ByteBuffer.wrap(baw.toByteArray()));
        }
        
        res = readByte(socketChannel);

        if (res != 0x05) {
            throw new IOException("Invalid response from SOCKS5 server (" +
                res + ") " + protocolContext.getProxyHostname() + ":" + protocolContext.getProxyPort());
        }
        
        int status = readByte(socketChannel);

        if (status != 0x00) {
            if ((status > 0) && (status < 9)) {
                throw new IOException(
                    "SOCKS5 server unable to connect, reason: " +
                    SOCKSV5_ERROR[status]);
            }
			throw new IOException(
			    "SOCKS5 server unable to connect, reason: " + status);
        }

        readByte(socketChannel);

        int aType = readByte(socketChannel);

        switch (aType) {
        case 0x01:

        	socketChannel.read(ByteBuffer.allocate(4));
            break;

        case 0x03:

            int n = readByte(socketChannel);
            socketChannel.read(ByteBuffer.allocate(n));

            break;

        default:
            throw new IOException("SOCKS5 gave unsupported address type: " +
                aType);
        }

        if(socketChannel.read(ByteBuffer.allocate(2)) != 2) {
        	throw new IOException("SOCKS5 error reading port");
        }

	}

	public boolean isStartupRequiresListeningInterfaces() {
		return startupRequiresListeningInterfaces;
	}

	public void setStartupRequiresListeningInterfaces(boolean startupRequiresListeningInterfaces) {
		this.startupRequiresListeningInterfaces = startupRequiresListeningInterfaces;
	}

	private <K extends ProtocolContext> ProtocolEngine registerClientConnection(K protocolContext, SocketChannel socketChannel, ConnectRequestFuture connectFuture) throws IOException {
		SocketHandler connection = protocolContext.getSocketConnectionFactory().createSocketConnection(
				context, 
				socketChannel.socket().getLocalSocketAddress(), 
				socketChannel.socket().getRemoteSocketAddress());
		ProtocolEngine engine = protocolContext.createEngine(connectFuture);
		connection.initialize(engine, SshEngine.this, socketChannel);
		registerHandler(connection, socketChannel);
		
		return engine;
	}

	/**
	 * Register a client connector with the daemon.
	 * 
	 * @param connector
	 *            ClientConnector
	 * @param socketChannel
	 *            SocketChannel
	 * @throws IOException
	 */
	public void registerConnector(ClientConnector connector,
			SocketChannel socketChannel) throws IOException {

		SelectorThread t = connectThreads.selectNextThread();
		t.register(socketChannel, SelectionKey.OP_CONNECT, connector, true);

	}

	/**
	 * Register a client acceptor with the daemon.
	 * 
	 * @param acceptor
	 *            ClientAcceptor
	 * @param socketChannel
	 *            ServerSocketChannel
	 * @throws IOException
	 */
	public void registerAcceptor(ClientAcceptor acceptor,
			ServerSocketChannel socketChannel) throws IOException {

		acceptThreads.register(socketChannel, SelectionKey.OP_ACCEPT, acceptor, true);
	}

	/**
	 * Register a socket handler with the daemon.
	 * 
	 * @param handler
	 *            SocketHandler
	 * @param channel
	 *            SelectableChannel
	 * @throws IOException
	 */
	public void registerHandler(SocketHandler handler, SelectableChannel channel)
			throws IOException {

		SelectorThread t = transferThreads.selectNextThread();
		registerHandler(handler, channel, t);
	}

	/**
	 * Register a socket handler with the daemon.
	 * 
	 * @param handler
	 *            SocketHandler
	 * @param channel
	 *            SelectableChannel
	 * @param thread
	 *            SelectorThread
	 * @throws IOException
	 */
	public void registerHandler(SocketHandler handler,
			SelectableChannel channel, SelectorThread thread)
			throws IOException {

		handler.setThread(thread);
		if (thread == null)
			throw new IOException("Unable to allocate thread");
		thread.register(channel, handler.getInitialOps(), handler, true);

	}

	class AcceptSelectorThread implements SelectorThreadImpl {

		public void processSelectionKey(SelectionKey key, SelectorThread thread) {

			ClientAcceptor acceptor = (ClientAcceptor) key.attachment();

			if(Log.isTraceEnabled())
				Log.trace(context.getBufferPool().getAllocatedBuffers()
						+ " direct buffers allocated, "
						+ context.getBufferPool().getFreeBuffers() + " free");
			acceptor.finishAccept(key);
		}
		
		public String getName() {
			return context.getProduct() + "-ACCEPT";
		}

	}
	
	class TransferSelectorThread implements SelectorThreadImpl {

		public void processSelectionKey(final SelectionKey key, SelectorThread t) {

			SocketHandler listener = (SocketHandler) key.attachment();

			if (key != null && key.isValid()) {
				key.interestOps(0);
			
				if(Log.isTraceEnabled()) {
					Log.trace("Processing {}{}{}", listener.getName(),
							key.isReadable() ? " READ" : "", key.isWritable() ? " WRITE" : "");
				}
				
				listener.addTask(new SocketReadWriteTask(listener.getConnection(), key, listener));
			}
		}
		
		public String getName() {
			return context.getProduct() + "-TRANSFER";
		}

	}

	class SocketReadWriteTask extends ConnectionAwareTask {
		
		SocketHandler listener;
		SelectionKey key;
		SocketReadWriteTask(Connection<?> con, SelectionKey key, SocketHandler listener) {
			super(con);
			this.key = key;
			this.listener = listener;
		}
		
		public void doTask() {
			
			boolean cancel = false;
			if (key.isValid() && key.isWritable()) {
				if(Log.isTraceEnabled()) {
					Log.trace("Starting {} WRITE", listener.getName());
				}
				cancel = listener.processWriteEvent();
			}

			if (key.isValid() && key.isReadable()) {
				if(Log.isTraceEnabled()) {
					Log.trace("Starting {} READ", listener.getName());
				}
				cancel |= listener.processReadEvent();
			}

			if(cancel) {
				key.cancel();
			} else {
				listener.getSelectorThread().addSelectorOperation(new Runnable() {
					public void run() {
						if(key.isValid()) {
							int ops = 0;
							boolean wantsWrite = listener.wantsWrite();
							boolean wantsRead =  listener.wantsRead();
							if(wantsWrite) {
								ops |= SelectionKey.OP_WRITE;
							}
							if(wantsRead) {
								ops |= SelectionKey.OP_READ;
							}
							if(Log.isTraceEnabled()) {
								Log.trace("{} has state ops={} {}",
										listener.getName(),
										ops,
										wantsWrite && wantsRead ? "READ/WRITE" : wantsWrite ? "WRITE" : wantsRead ? "READ" : "NONE");
							}
							key.interestOps(ops); 
						}
					}
				});
			}
		
		}
	}
		
	class ConnectSelectorThread implements SelectorThreadImpl {

		public void processSelectionKey(SelectionKey key, SelectorThread thread) {
			ClientConnector con = (ClientConnector) key.attachment();
			if(con.finishConnect(key)) {
				key.cancel();
			}
		}

		public String getName() {
			return context.getProduct() + "-CONNECT";
		}
	}

	class DaemonClientConnector implements ClientConnector {

		ProtocolContext protocolContext;
		SocketChannel socketChannel;
		ProtocolEngine engine;
		ConnectRequestFuture connectFuture;
		String hostToConnect;
		int portToConnect;
		DaemonClientConnector(ProtocolContext protocolContext, SocketChannel socketChannel, ConnectRequestFuture connectFuture,
				String hostToConnect, int portToConnect) {
			this.protocolContext = protocolContext;
			this.socketChannel = socketChannel;
			this.connectFuture = connectFuture;
			this.hostToConnect = hostToConnect;
			this.portToConnect = portToConnect;
		}
		
		public void registrationCompleted(SelectableChannel channel, 
				SelectionKey key, SelectorThread selectorThread) {
			
		}

		public boolean finishConnect(SelectionKey key) {
			
			try {
				while (!socketChannel.finishConnect()) {
				    // Wait for the connection to complete
				}
				processOpenSocket(socketChannel, protocolContext, hostToConnect, portToConnect);
				engine = registerClientConnection(protocolContext, socketChannel, connectFuture);
				return true;
			} catch (Exception e) {
				Log.error("Failed to connect socket", e);
				return false;
			} finally {
				key.cancel();
			}
		}
		
	}
	
	class ProtocolClientAcceptor extends ClientAcceptor {

		ServerSocketChannel socketChannel;
		ListeningInterface li;

		ProtocolClientAcceptor(ListeningInterface li,
				ServerSocketChannel socketChannel) {
			super(li);
			this.li = li;
			this.socketChannel = socketChannel;
		}

		public boolean finishAccept(SelectionKey key,
				ListeningInterface li) {

			SocketChannel sc = null;
			boolean registered = false;

			try {
				EventServiceImplementation.getInstance().fireEvent(
						(new Event(this, EventCodes.EVENT_CONNECTION_ATTEMPT,
								true)).addAttribute(EventCodes.ATTRIBUTE_IP,
								((ServerSocketChannel) key.channel()).socket()
										.getInetAddress().getHostAddress()));

				sc = ((ServerSocketChannel) key.channel()).accept();

				if (sc != null) {
					
					ProtocolContext protocolContext = li.getContextFactory().createContext(context, sc);
					
					sc.socket().setKeepAlive(
							protocolContext.getSocketOptionKeepAlive());
					sc.socket().setTcpNoDelay(
							protocolContext.getSocketOptionTcpNoDelay());
					
					if(protocolContext.getSendBufferSize() > 0) {
						sc.socket().setSendBufferSize(
							protocolContext.getSendBufferSize());
					}
					
					if(protocolContext.getReceiveBufferSize() > 0) {
						sc.socket().setReceiveBufferSize(
							protocolContext.getReceiveBufferSize());
					}
					
					sc.configureBlocking(false);

					if(Log.isWarnEnabled() && protocolContext.getReceiveBufferSize() > 0) {
						if(sc.socket().getReceiveBufferSize()!=protocolContext.getReceiveBufferSize()) {
							Log.warn("WARNING: TCP receive buffer could not be set to "
									+ protocolContext.getReceiveBufferSize()
									+ ". The socket reported a size of "
									+ sc.socket().getReceiveBufferSize());
						}
					}
					
					if(Log.isWarnEnabled() && protocolContext.getSendBufferSize() > 0) {
						if(sc.socket().getSendBufferSize()!=protocolContext.getSendBufferSize()) {
							Log.warn("WARNING: TCP send buffer could not be set to "
									+ protocolContext.getSendBufferSize()
									+ ". The socket reported a size of "
									+ sc.socket().getSendBufferSize());
						}

					}

		        	SocketHandler connection = protocolContext.getSocketConnectionFactory().createSocketConnection(
		        			context, 
		        			sc.socket().getLocalSocketAddress(), 
		        			sc.socket().getRemoteSocketAddress());
		        	ProtocolEngine e = protocolContext.createEngine(new ConnectRequestFuture());
		            connection.initialize(e, SshEngine.this, sc);
		        	registerHandler(connection, sc);
			        
					registered = true;

					return !((ServerSocketChannel) key.channel()).isOpen();
				} else {
					if(Log.isInfoEnabled())
						Log.info("Accept event fired but no socket was accepted");
					
					return true;
				}
			} catch (Throwable ex) {
				if(Log.isInfoEnabled())
					Log.info("SSH client acceptor failed to accept", ex);

				if (sc != null && !registered) {

					try {
						sc.close();
					} catch (IOException e) {
					}
					try {
						sc.socket().close();
					} catch (IOException e) {
					}
				}
				
				return !((ServerSocketChannel) key.channel()).isOpen();
			}

			

		}

		public void stopAccepting() throws IOException {
			socketChannel.close();
		}

	}

	public static SshEngine getDefaultInstance() throws IOException {

		synchronized(SshEngine.class) {
			if(defaultInstance==null) {
				defaultInstance = new SshEngine();
				if(!defaultInstance.startup()) {
					throw new IOException("Failed to start SSH engine");
				}
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						defaultInstance.shutdownNow(false, 0L);
					}
				});
				return defaultInstance;
			}
	
			if(!defaultInstance.isStarted()) {
				defaultInstance.startup();
			}
			return defaultInstance;
		}
	}

	public AbstractRequestFuture getShutdownFuture() {
		return shutdownFuture;
	}
}
