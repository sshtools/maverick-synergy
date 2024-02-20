package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.RequestFutureListener;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.nio.ClientAcceptor;
import com.sshtools.synergy.nio.ListeningInterface;

/**
 * This class implements the standard socket based forwarding for the SSHD.
 */
public abstract class SocketListeningForwardingChannelFactoryImpl<T extends SshContext>
      extends ClientAcceptor implements ForwardingChannelFactory<T> {

    protected String addressToBind;
    protected int portToBind;
    protected ServerSocketChannel socketChannel;
    protected ConnectionProtocol<T> connection;
    protected SocketAddress addr;
    protected String channelType;
    protected ActiveTunnelManager<T> activeRemoteForwardings = new ActiveTunnelManager<T>();

    public SocketListeningForwardingChannelFactoryImpl() {
      super(null);
    }
    
    public ActiveTunnelManager<T> getActiveTunnelManager() {
    	return activeRemoteForwardings;
    }

    public boolean belongsTo(ConnectionProtocol<T> connection) {
        return this.connection!=null && this.connection.equals(connection);
    }

    public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<T> connection) throws IOException {
    	return bindInterface(addressToBind, portToBind, connection, getChannelType());
    }
    
    @SuppressWarnings("unchecked")
	public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType) throws IOException {

        this.addressToBind = addressToBind;
        this.portToBind = portToBind;
        this.connection = (ConnectionProtocol<T>) connection;
        this.channelType = channelType;
        
        addr = new InetSocketAddress(addressToBind, portToBind);

        this.socketChannel = ServerSocketChannel.open();
        
        try {
	        socketChannel.configureBlocking(false);
	        socketChannel.socket().setReuseAddress(true);
	        if(connection.getContext().getReceiveBufferSize() > 0) {
	        	socketChannel.socket().setReceiveBufferSize(
	        			connection.getContext().getReceiveBufferSize());
	        }
	        ServerSocket socket = socketChannel.socket();
	        socket.bind(addr, connection.getContext().getMaximumSocketsBacklogPerRemotelyForwardedConnection());
	
	        connection.getContext().getEngine().registerAcceptor(this, socketChannel);
	        
	        return this.portToBind = socketChannel.socket().getLocalPort();
        
        } catch(IOException e) {
			IOUtils.closeStream(socketChannel);
			throw e;
        }
    }

    public boolean finishAccept(SelectionKey key, ListeningInterface li) {
      try {
        final SocketChannel sc = socketChannel.accept();
        
        if(sc!=null) {

            if(Log.isDebugEnabled()) { 
            	Log.debug(channelType + " forwarding socket accepted from "
		              + ((InetSocketAddress)sc.socket().getRemoteSocketAddress()).getAddress().getHostAddress()
		              + "/"
		              + ((InetSocketAddress)sc.socket().getRemoteSocketAddress()).getAddress().getHostAddress()
		              + ":"
		              + ((InetSocketAddress)sc.socket().getRemoteSocketAddress()).getPort());
            }
            sc.configureBlocking(false);
            if(connection.getContext().getReceiveBufferSize() > 0) {
            	sc.socket().setReceiveBufferSize(connection.getContext().getReceiveBufferSize());
            }
            if(connection.getContext().getSendBufferSize() > 0) {
            	sc.socket().setSendBufferSize(connection.getContext().getSendBufferSize());
            }
            sc.socket().setKeepAlive(connection.getContext().getSocketOptionKeepAlive());
            sc.socket().setTcpNoDelay(connection.getContext().getSocketOptionTcpNoDelay());

            ForwardingChannel<T> channel = createChannel(channelType,
            		connection.getTransport().getConnection(),
                    addressToBind,
                    portToBind,
                    sc,
                    connection.getContext());

            channel.addEventListener(activeRemoteForwardings);

            channel.getOpenFuture().addFutureListener(new RequestFutureListener() {
            	public void complete(RequestFuture future) {
            		
            		if(!future.isSuccess()) {
	            		if(Log.isDebugEnabled()) {
	                    	Log.debug("Channel could not be opened");
	                    }
	                    try {
	                        sc.close();
	                    } catch(IOException ex) { }
            		}
            	}
            });
            
            connection.openChannel(channel);

        } else {
            if(Log.isDebugEnabled()) {
            	Log.debug("FORWARDING accept event fired but no socket was accepted");
            }
        }
      }
      catch (IOException ex) {
        if(Log.isDebugEnabled()) {
        	Log.debug("Accept operation failed on " + addressToBind + ":" + portToBind, ex);
        }
      }

      return !socketChannel.isOpen();
    }

    protected abstract ForwardingChannel<T> createChannel(String channelType,
    		SshConnection con, 	String addressToBind, int portToBind, SocketChannel sc, T context);

	public void stopListening(boolean dropActiveTunnels) {
     
	  stopAccepting();

      if(dropActiveTunnels) {
          activeRemoteForwardings.killAllTunnels();
      }

    }

	public void stopAccepting() {
		try {
			socketChannel.close();
		} catch (Throwable e) {
			Log.error("Error closing listening socket", e);
		}
	}

	public static class ActiveTunnelManager<K extends SshContext> implements ChannelEventListener {

		public interface TunnelListener<K extends SshContext> {

			void tunnelOpened(ForwardingChannel<K> channel);
		}

		List<Channel> activeTunnels = Collections.synchronizedList(new ArrayList<>());
		List<TunnelListener<K>> listeners = Collections.synchronizedList(new ArrayList<>());
		boolean killingTunnels = false;
		
		public void addListener(TunnelListener<K> listener) {
			listeners.add(listener);
		}
		
		public void removeListener(TunnelListener<K> listener) {
			listeners.remove(listener);
		}

		public List<Channel> getTunnels() {
			return activeTunnels;
		}

		public void killAllTunnels() {

			synchronized (activeTunnels) {
				killingTunnels = true;
				for (Channel channel : activeTunnels) {
					try {
						channel.close();
					} catch (Throwable t) {
					}
				}
				activeTunnels.clear();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onChannelOpen(Channel channel) {
			synchronized (activeTunnels) {
				if (!killingTunnels)
					activeTunnels.add(channel);
				for(int i = listeners.size() - 1 ; i >= 0 ; i--) 
					listeners.get(i).tunnelOpened((ForwardingChannel<K>) channel);
			}
		}

		@Override
		public void onChannelClose(Channel channel) {
			synchronized (activeTunnels) {
				if (!killingTunnels)
					activeTunnels.remove(channel);
			}
		}
	}


}
