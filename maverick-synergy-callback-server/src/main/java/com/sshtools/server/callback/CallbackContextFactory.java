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
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.SshEngineContext;

public class CallbackContextFactory implements ProtocolContextFactory<SshClientContext> {

	public static final String CALLBACK_IDENTIFIER = "CallbackClient-";
	
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
	
	@Override
	public SshClientContext createContext(SshEngineContext daemonContext, SocketChannel sc)
			throws IOException, SshException {
		
		
		SshClientContext clientContext = new SwitchingSshContext(
				daemonContext.getEngine(), callbackIdentifier, new ProtocolContextFactory<SshServerContext>() {

					@Override
					public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
							throws IOException, SshException {
						
						SshServerContext serverContext=  new SshServerContext(daemonContext.getEngine());
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
		
		configureClientContext(clientContext);
		return clientContext;
	}

	protected void configureClientContext(SshClientContext clientContext) {
		
	}

	public void setCallbackIdentifier(String callbackIdentifier) {
		this.callbackIdentifier = callbackIdentifier;
	}

	public void setMutualAuthenticationStore(MutualKeyAuthenticatonStore store) {
		this.store = store;
		
	}

	protected void configureServerContext(SshServerContext serverContext) {
		
	}
}