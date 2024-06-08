package com.sshtools.callback.client;

/*-
 * #%L
 * Callback Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.ServerConnectionStateListener;
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
		while(app.getSshEngine().isStarted()) {
			
			if(isStopped) {
				Log.info("Callback to {}:{} has been stopped", hostname, port);
				break;
			}
			
			try {
				connect();
			} catch (IOException | SshException e) {
				Log.error("Connection failed to {}:{}", hostname, port);
			}
			
			if(Log.isInfoEnabled()) {
				Log.info("Connection disconnected from {}:{}", hostname, port);
			}
			
			if(!config.isReconnect()) {
				break;
			}
			
			try {
				long interval = config.getReconnectIntervalMs();

				if(Log.isInfoEnabled()) {
					Log.info("Will reconnect to {}:{} in {} seconds", hostname, port, interval / 1000);
				}
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
		} 
	}

	public void connect() throws IOException, SshException {
		
		if(Log.isInfoEnabled()) {
			Log.info("Connecting to {}:{}", hostname, port);
		}
		
//		synchronized(app) {
//			if(!app.getSshEngine().isStarted() && !app.getSshEngine().isStarting()) {
//				if(!app.getSshEngine().startup()) {
//					throw new IOException("SSH Engine failed to start");
//				}
//			}
//		}

		
		SshConnection con = null;

		future = app.getSshEngine().connect(
				hostname, 
				port, 
				createContext(config));
		
		future.waitFor(30000L);
		if(future.isDone() && future.isSuccess()) {
		
			con = future.getConnection();
			con.setProperty(CallbackClient.CALLBACK_CLIENT, CallbackSession.this);
			con.getAuthenticatedFuture().waitFor(30000L);
			
			if(con.getAuthenticatedFuture().isDone()
					&& con.getAuthenticatedFuture().isSuccess()) {
			
				if(Log.isInfoEnabled()) {
					Log.info("Callback {} registering with memo {}", con.getUUID(), config.getMemo());
				}
				GlobalRequest req = new GlobalRequest("memo@jadaptive.com", 
						con, ByteArrayWriter.encodeString(config.getMemo()));
				con.sendGlobalRequest(req, false);
				app.onClientConnected(this, con);
				if(Log.isInfoEnabled()) {
					Log.info("Client is connected to {}:{}", hostname, port);
				}

				con.getDisconnectFuture().waitForever();
			} else {
				if(Log.isInfoEnabled()) {
					Log.info("Could not authenticate to {}:{}", hostname, port);
				}
				con.disconnect();
			}
			
			app.onClientStop(this, con);
			con.removeProperty(CallbackClient.CALLBACK_CLIENT);
			app.clients.remove(this);
		}

		
	}
		
	protected ProtocolContext createContext(CallbackConfiguration config) throws IOException, SshException {
		return app.createContext(app.getSshEngine().getContext(), config);
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
