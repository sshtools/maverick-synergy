/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.callback.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.KeyboardInteractiveAuthenticationProvider;
import com.sshtools.common.auth.KeyboardInteractiveProvider;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.DisconnectRequestFuture;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionProtocol;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.GlobalRequestHandler;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;

/**
 * Implements a reverse SSH server. Making a client socket connection out to the CallbackServer which is listening
 * on the SSH port to act as a client to any incoming connections. The connection is authenticated by a public
 * key held by the CallbackClient.
 */
public abstract class CallbackClient<T extends CallbackConfiguration> implements ProtocolContextFactory<SshServerContext>, Runnable {

	

	ServerPublicKeyAuthentication serverAuthenticationProvider;
	T config;
	CallbackApplication<T> app;
	ConnectRequestFuture future;
	Connection<?> currentConnection;
	boolean isStopped = false;
	public static final String REMOTE_UUID = "remoteUUID";
	String hostname;
	int port;
	boolean onDemand = false;
	Map<String,Object> attributes = new HashMap<String,Object>();
	int numberOfAuthenticationErrors = 0;
	
	public CallbackClient(T config, CallbackApplication<T> app, String hostname, int port, boolean onDemand) throws IOException {
		this.config = config;
		this.app = app;
		this.onDemand = onDemand;
		this.hostname = hostname;
		this.port = port;
		serverAuthenticationProvider = new ServerPublicKeyAuthentication(config.getAuthorizedKeys());
	}
	
	public void run() {
		try {
			connect();
		} catch (IOException e) {
			Log.error("Failed to startup", e);
		}
	}

	public void connect() throws IOException {
		
		if(isStopped) {
			throw new IOException("Client has been stopped");
		}
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Connecting to %s:%d", hostname, port));
		}
		
		synchronized(app) {
			if(!app.getSshEngine().isStarted() && !app.getSshEngine().isStarting()) {
				if(!app.getSshEngine().startup()) {
					throw new IOException("SSH Engine failed to start");
				}
			}
		}
		int count = 1;
		while(app.getSshEngine().isStarted()) {
			try {
				future = app.getSshEngine().connect(
						hostname, 
						port, 
						createContext(app.getSshEngine().getContext(), null));;
				future.waitFor(30000L);
				if(future.isDone() && future.isSuccess()) {
					TransportProtocol<?> transport = (TransportProtocol<?>)future.getTransport();
					currentConnection = transport.getConnection();
					currentConnection.getAuthenticatedFuture().waitFor(30000L);
					if(currentConnection.getAuthenticatedFuture().isDone() && currentConnection.getAuthenticatedFuture().isSuccess()) {
						currentConnection.setProperty("callbackClient", this);
						app.onClientConnected(this);
						if(Log.isInfoEnabled()) {
							Log.info(String.format("Client is connected to %s:%d", hostname, port));
						}
						numberOfAuthenticationErrors = 0;
						break;
					} else {
						if(Log.isInfoEnabled()) {
							Log.info(String.format("Could not authenticate to %s:%d", hostname, port));
						}
						currentConnection.disconnect();
						numberOfAuthenticationErrors++;
					}
				}
				try {
					long interval = config.getReconnectIntervalMs();
					if(numberOfAuthenticationErrors >= 3) {
						interval = TimeUnit.MINUTES.toMillis(10);
					}
					if(numberOfAuthenticationErrors >= 9) {
						interval = TimeUnit.MINUTES.toMillis(60);
					}
					if(Log.isInfoEnabled()) {
						Log.info(String.format("Will reconnect to %s:%d in %d seconds", hostname, port, interval / 1000));
					}
					Thread.sleep(interval);
				} catch (InterruptedException e) {
				}
			} catch(Throwable e) {
				Log.error(String.format("%s on %s:%d", 
						e.getMessage(),
						config.getServerHost(), 
						config.getServerPort()));
				long interval = config.getReconnectIntervalMs() * Math.min(count, 12 * 60);
				if(Log.isInfoEnabled()) {
					Log.info(String.format("Reconnecting to %s:%d in %d seconds", hostname, port, interval / 1000));
				}
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e1) {
				}
				if(count >= 12) {
					count += 12;
				} else {
					count++;
				}
			}
		}
	}
	
	public boolean startRoute(String name, int startPort, int endPort, String destinationHost, int destinationPort) throws IOException {
		if(currentConnection==null) {
			throw new IOException("Client is not connected");
		}
		
		ByteArrayWriter msg = new ByteArrayWriter();
		
		try {
			msg.writeString(config.getAgentName());
			msg.writeString(name);
			msg.writeString(destinationHost);
			msg.writeInt(destinationPort);
			msg.writeInt(startPort);
			msg.writeInt(endPort);
			
			GlobalRequest request = new GlobalRequest("start-route@hypersocket.com", currentConnection, msg.toByteArray());
			currentConnection.getConnectionProtocol().sendGlobalRequest(request, true);
			
			request.waitFor(60000);
			
			return request.isDone() && request.isSuccess();
		
		} finally {
			msg.close();
		}
		
		
	}
	
	public boolean stopRoute(String name) throws IOException {
		
		if(currentConnection==null) {
			throw new IOException("Client is not connected");
		}
		
		ByteArrayWriter msg = new ByteArrayWriter();
		
		try {
			msg.writeString(config.getAgentName());
			msg.writeString(name);
			
			GlobalRequest request = new GlobalRequest("stop-route@hypersocket.com", currentConnection, msg.toByteArray());
			currentConnection.getConnectionProtocol().sendGlobalRequest(request, true);
			
			request.waitFor(60000);
			
			return request.isDone() && request.isSuccess();
		} finally {
			msg.close();
		}
	}
	
	public void disconnect() {
		if(future.isDone() && future.isSuccess()) {
			future.getTransport().disconnect(TransportProtocol.BY_APPLICATION, "The user disconnected.");
		}
		currentConnection = null;
	}
	
	public DisconnectRequestFuture stop() {
		isStopped = true;
		disconnect();
		return future.getTransport().getDisconnectFuture();
	}
	
	public boolean authenticateUUID(String remoteUUID) throws IOException {
		
		if(config.getRemoteUUID()==null) {
			config.setRemoteUUID(remoteUUID);
		}
		
		if(Log.isDebugEnabled()) {
			Log.debug(String.format("Authenticating remote UUD %s against expected %s", config.getRemoteUUID(), remoteUUID));
		}
		return config.getRemoteUUID().equals(remoteUUID);
	}
	
	@Override
	public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc) throws IOException, SshException {
		
		SshServerContext sshContext = new SshServerContext(app.getSshEngine(), JCEComponentManager.getDefaultInstance());

		for(SshKeyPair key : config.getHostKeys()) {
			sshContext.addHostKey(key);
		}
		
		sshContext.setSoftwareVersionComments("CallbackClient-" + SshEngine.getVersion());

		sshContext.getPolicy(AuthenticationMechanismFactory.class).addProvider(serverAuthenticationProvider);
		sshContext.getPolicy(AuthenticationMechanismFactory.class).addProvider(new KeyboardInteractiveAuthenticationProvider() {
			
			public KeyboardInteractiveProvider createInstance(SshConnection con) {
				return new KeyboardInteractiveAuthentication(CallbackClient.this);
			}
		});
		
		sshContext.getPolicy(AuthenticationPolicy.class).addRequiredMechanism("publickey");
		sshContext.getPolicy(AuthenticationPolicy.class).addRequiredMechanism("keyboard-interactive");
		
		sshContext.setSendIgnorePacketOnIdle(true);
		
		sshContext.addGlobalRequestHandler(new GlobalRequestHandler<SshServerContext>() {
			
			@Override
			public String[] supportedRequests() {
				return new String[] {"broker-connection@hypersocket.com",
						"update@hypersocket.com",
						"broker-query@hypersocket.com"};
			}
			
			@Override
			public boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<SshServerContext> context) {
				
				ByteArrayReader msg = new ByteArrayReader(request.getData());
				
				try {
					if(request.getName().equals("broker-connection@hypersocket.com")) {
						String hostname = msg.readString();
						int port = (int) msg.readInt();
						brokerConnection(hostname, port);
						return true;
					} else if(request.getName().equals("broker-query@hypersocket.com")) {
						return !onDemand;
					} else if(request.getName().equals("update@hypersocket.com")) {
						if(!Boolean.getBoolean("hypersocket.development")) {
							onUpdateRequest(context.getConnection(), msg.readString());
						}
						return true;
					} else {
						return false;
					}
				} catch (IOException e) {
					return false;
				} finally {
					msg.close();
				}
			}

		});
		
		sshContext.getForwardingPolicy().allowForwarding();
		
		configureContext(sshContext);
		
		return sshContext;
	}
	
	protected abstract void onUpdateRequest(Connection<?> con, String updateInfo);
	
	protected abstract void configureContext(SshServerContext sshContext) throws IOException, SshException;

	public String getUUID() {
		return config.getLocalUUID();
	}
	
	public String getName() {
		return config.getAgentName() + "@" + config.getServerHost();
	}

	public T getConfig() {
		return config;
	}

	public boolean isStopped() {
		return isStopped;
	}

	public void setConfig(T config) {
		this.config = config;
	}
	
	private void brokerConnection(String hostname, int port) throws IOException {
		app.start(app.createClient(config, hostname, port, true));
	}
	
	public boolean hasAttribute(String key) {
		return attributes.containsKey(key);
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key);
	}
	
	public void setAttribute(String key, Object val) {
		attributes.put(key, val);
	}
}
