/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.DisconnectRequestFuture;
import com.sshtools.synergy.nio.ProtocolContext;
import com.sshtools.synergy.ssh.TransportProtocol;

/**
 * Implements a reverse SSH server. Making a client socket connection out to the CallbackServer which is listening
 * on the SSH port to act as a client to any incoming connections. The connection is authenticated by a public
 * key held by the CallbackClient.
 */
public class CallbackSession implements Runnable {

	public static final String CALLBACK_IDENTIFIER = "CallbackClient-";

	CallbackConfiguration config;
	CallbackClient app;
	ConnectRequestFuture future;
	
	boolean isStopped = false;
	String hostname;
	int port;
	Map<String,Object> attributes = new HashMap<String,Object>();
	int numberOfAuthenticationErrors = 0;
	
	public CallbackSession(CallbackConfiguration config, CallbackClient app, String hostname, int port) throws IOException {
		this.config = config;
		this.app = app;
		this.hostname = hostname;
		this.port = port;
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
			Log.info("Connecting to {}:{}", hostname, port);
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
						createContext(config));
				future.waitFor(30000L);
				if(future.isDone() && future.isSuccess()) {
					SshConnection currentConnection = future.getConnection();
					currentConnection.getAuthenticatedFuture().waitFor(30000L);
					if(currentConnection.getAuthenticatedFuture().isDone() && currentConnection.getAuthenticatedFuture().isSuccess()) {
						currentConnection.setProperty("callbackClient", this);

						GlobalRequest req = new GlobalRequest("memo@jadaptive.com", 
								currentConnection, ByteArrayWriter.encodeString(config.getMemo()));
						currentConnection.sendGlobalRequest(req, false);
						app.onClientConnected(this, currentConnection);

						if(Log.isInfoEnabled()) {
							Log.info("Client is connected to {}:{}", hostname, port);
						}
						numberOfAuthenticationErrors = 0;
						break;
					} else {
						if(Log.isInfoEnabled()) {
							Log.info("Could not authenticate to {}:{}", hostname, port);
						}
						currentConnection.disconnect();
						numberOfAuthenticationErrors++;
					}
				}
				
				if(!config.isReconnect()) {
					break;
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
						Log.info("Will reconnect to {}:{} in {} seconds", hostname, port, interval / 1000);
					}
					Thread.sleep(interval);
				} catch (InterruptedException e) {
				}
			} catch(Throwable e) {
				Log.error("{} on {}:{}", 
						e,
						e.getMessage(),
						config.getServerHost(), 
						config.getServerPort());
				long interval = config.getReconnectIntervalMs() * Math.min(count, 12 * 60);
				if(Log.isInfoEnabled()) {
					Log.info("Reconnecting to {}:{} in {} seconds", hostname, port, interval / 1000);
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
		
	protected ProtocolContext createContext(CallbackConfiguration config) throws IOException, SshException {
		SshServerContext ctx = app.createContext(app.getSshEngine().getContext(), config);
		return ctx;
	}

	public void disconnect() {
		if(future.isDone() && future.isSuccess()) {
			future.getTransport().disconnect(TransportProtocol.BY_APPLICATION, "The user disconnected.");
		}
	}
	
	public DisconnectRequestFuture stop() {
		isStopped = true;
		disconnect();
		return future.getTransport().getDisconnectFuture();
	}

	public String getName() {
		return config.getAgentName() + "@" + config.getServerHost();
	}

	public CallbackConfiguration getConfig() {
		return config;
	}

	public boolean isStopped() {
		return isStopped;
	}

	public void setConfig(CallbackConfiguration config) {
		this.config = config;
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
