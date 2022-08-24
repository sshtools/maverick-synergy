/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.server.callback;

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
			public byte[] processGlobalRequest(GlobalRequest request, ConnectionProtocol<SshClientContext> connection, boolean wantreply) throws GlobalRequestHandlerException {
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
					return null;
				}
				throw new GlobalRequestHandler.GlobalRequestHandlerException();
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
		server.configure(serverContext, null);
	}
}