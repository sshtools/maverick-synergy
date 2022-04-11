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
package com.sshtools.server.callback;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sshtools.client.AuthenticationProtocolClient;
import com.sshtools.client.ClientStateListener;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.auth.MutualKeyAuthenticatonStore;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.SshEngineContext;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.GlobalRequestHandler;

public class CallbackContextFactory implements ProtocolContextFactory<SshClientContext> {

	public static final String CALLBACK_IDENTIFIER = "CallbackClient_";
	
	public static final String CALLBACK_MEMO = "MEMO";
	
	String callbackIdentifier = CALLBACK_IDENTIFIER;
	Map<String,SshConnection> callbackClients = new HashMap<>();
	
	MutualKeyAuthenticatonStore store;
	
	public CallbackContextFactory(MutualKeyAuthenticatonStore store) {
		this.store = store;
	}

	public Collection<SshConnection> getCallbackClients() {
		return Collections.unmodifiableCollection(callbackClients.values());
	}
	
	public SshConnection getCallbackClient(String username) {
		return callbackClients.get(username);
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
						configureServerContext(serverContext);
						return serverContext;
					}
				});
		
		clientContext.addAuthenticator(new MutualCallbackAuthenticator(store));
		clientContext.addStateListener(new ClientStateListener() {

			@Override
			public void authenticationStarted(AuthenticationProtocolClient authClient,
					SshConnection con) {
				if(callbackClients.containsKey(con.getUsername())) {
					con.disconnect(String.format("Only one connection allowed by %s at anyone time", con.getUsername()));
				}
			}

			@Override
			public void connected(SshConnection con) {
				Log.info("Callback client {} connected", con.getUsername());
				callbackClients.put(con.getUsername(), con);
			}

			@Override
			public void disconnected(SshConnection con) {
				SshConnection connected = callbackClients.get(con.getUsername());
				if(Objects.nonNull(connected)) {
					if(connected.equals(con)) {
						Log.info("Callback client {} disconnected", con.getUsername());
						callbackClients.remove(con.getUsername());
					}
				}
				
			}
		});
		
		clientContext.addGlobalRequestHandler(new GlobalRequestHandler<SshClientContext>() {
			
			@Override
			public String[] supportedRequests() {
				return new String[] { "memo@jadaptive.com" };
			}
			
			@Override
			public boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<SshClientContext> connection) {
				if("memo@jadaptive.com".equals(request.getName())) {
					try {
						String memo = ByteArrayReader.decodeString(request.getData());
						if(Log.isInfoEnabled()) {
							Log.info("Callback {} registered with memo {}", connection.getUUID(), memo);
						}
						connection.getConnection().setProperty(CALLBACK_MEMO, memo);
					} catch (IOException e) {
					}
					return true;
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

	protected void configureServerContext(SshServerContext serverContext) throws IOException, SshException {
		
	}
}