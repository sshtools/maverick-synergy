package com.sshtools.server.callback;

/*-
 * #%L
 * Callback Server API
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
import java.nio.channels.SocketChannel;

import com.sshtools.client.ClientStateListener;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.auth.MutualKeyAuthenticatonStore;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.AbstractSshServer;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.SshEngineContext;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.GlobalRequestHandler;

public class CallbackContextFactory implements ProtocolContextFactory<SshClientContext> {

	public static final String CALLBACK_IDENTIFIER = "CallbackClient";
	
	public static final String CALLBACK_MEMO = "MEMO";
	
	String callbackIdentifier = CALLBACK_IDENTIFIER;
	
	MutualKeyAuthenticatonStore store;
	CallbackRegistrationService callbacks;
	AbstractSshServer server;
	
	public CallbackContextFactory(MutualKeyAuthenticatonStore store, 
			CallbackRegistrationService callbacks,
			AbstractSshServer server) {
		this.store = store;
		this.callbacks = callbacks;
		this.server = server;
	}
	
	protected SshServerContext createServerContext(SshEngineContext daemonContext) throws IOException, SshException {
		return new SshServerContext(daemonContext.getEngine());
	}
	
	@Override
	public SshClientContext createContext(SshEngineContext daemonContext, SocketChannel sc)
			throws IOException, SshException {
		
		
		SshClientContext clientContext = new SwitchingSshContext(
				daemonContext.getEngine(), callbackIdentifier, new ProtocolContextFactory<SshServerContext>() {

					@Override
					public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
							throws IOException, SshException {
						
						SshServerContext serverContext=  createServerContext(daemonContext);
						configureServerContext(serverContext, sc);
						return serverContext;
					}
				});
		
		clientContext.addAuthenticator(new MutualCallbackAuthenticator(store));
		clientContext.addStateListener(new ClientStateListener() {

			@Override
			public void connected(SshConnection con) {
				Log.info("Callback client {} connected", con.getUsername());
				callbacks.registerCallbackClient(con);
			}

			@Override
			public void disconnected(SshConnection con) {
				callbacks.unregisterCallbackClient(con.getUUID());
			}
		});
		
		clientContext.addGlobalRequestHandler(new GlobalRequestHandler<SshClientContext>() {
			
			@Override
			public String[] supportedRequests() {
				return new String[] { "memo@jadaptive.com" };
			}
			
			@Override
			public boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<SshClientContext> connection, 
					boolean wantreply, ByteArrayWriter out) throws GlobalRequestHandlerException {
				if("memo@jadaptive.com".equals(request.getName())) {
					try {
						String memo = ByteArrayReader.decodeString(request.getData());
						if(Log.isInfoEnabled()) {
							Log.info("Callback {} registered with memo {}", connection.getUUID(), memo);
						}
						Callback c = callbacks.getCallbackByUUID(connection.getUUID());
						connection.getConnection().setProperty(CALLBACK_MEMO, memo);
						c.setMemo(memo);
					} catch (IOException e) {
					}
					return false;
				}
				return false;
			}
		});
		configureCallbackContext(clientContext);
		return clientContext;
	}

	protected void configureCallbackContext(SshClientContext clientContext) {
		
	}

	public void setCallbackIdentifier(String callbackIdentifier) {
		this.callbackIdentifier = callbackIdentifier;
	}

	public void setMutualAuthenticationStore(MutualKeyAuthenticatonStore store) {
		this.store = store;
		
	}

	protected void configureServerContext(SshServerContext serverContext, SocketChannel sc) throws IOException, SshException {
		server.configure(serverContext, sc);
	}
}
