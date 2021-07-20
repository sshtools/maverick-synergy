
package com.sshtools.server.callback;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sshtools.client.AuthenticationProtocolClient;
import com.sshtools.client.ClientStateListener;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.auth.InMemoryMutualKeyAuthenticationStore;
import com.sshtools.common.auth.MutualKeyAuthenticatonStore;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.AbstractSshServer;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.SshEngineContext;

/**
 * An abstract server that provides a callback facility, listening on a port and acting as a client to 
 * any callback clients that connect to it. The callback client similarly acts as a server allowing 
 * this server to perform operations on the remote client.
 * 
 * The server also has the facility to act as a normal server. Switching modes depending on the 
 * client identifier provided by the SSH client.
 */
public class CallbackServer extends AbstractSshServer {

	public static final String CALLBACK_IDENTIFIER = "CallbackClient-";
	
	String callbackIdentifier = CALLBACK_IDENTIFIER;
	MutualKeyAuthenticatonStore authenticationStore = new InMemoryMutualKeyAuthenticationStore();
	ClientContextFactory defaultContextFactory = new ClientContextFactory();
	Map<String,SshConnection> callbackClients = new HashMap<>();
	
	public CallbackServer() {
	}
	
	public CallbackServer(InetAddress addressToBind, int port) {
		super(addressToBind, port);
	}
	
	public CallbackServer(int port) throws UnknownHostException {
		super(port);
	}

	public CallbackServer(String addressToBind, int port) throws UnknownHostException {
		super(addressToBind, port);
	}

	public void setCallbackIdentifier(String callbackIdentifier) {
		this.callbackIdentifier = callbackIdentifier;
	}
	
	public void setMutualKeyAuthenticationStore(MutualKeyAuthenticatonStore authenticationStore) {
		this.authenticationStore = authenticationStore;
	}
	
	public Collection<SshConnection> getCallbackClients() {
		return Collections.unmodifiableCollection(callbackClients.values());
	}
	
	public SshConnection getCallbackClient(String username) {
		return callbackClients.get(username);
	}
	
	protected void configureClientContext(SshClientContext clientContext) {
		
	}

	protected class ClientContextFactory implements ProtocolContextFactory<SshClientContext> {

		public ClientContextFactory() {
		}

		@Override
		public SshClientContext createContext(SshEngineContext daemonContext, SocketChannel sc)
				throws IOException, SshException {
			SshClientContext clientContext = new SwitchingSshContext(
					getEngine(), callbackIdentifier, new ServerContextFactory());
			configureClientContext(clientContext);
			clientContext.addAuthenticator(new MutualCallbackAuthenticator(authenticationStore));
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
			return clientContext;
		}
	}
	
	class ServerContextFactory implements ProtocolContextFactory<SshServerContext> {

		@Override
		public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
				throws IOException, SshException {
			SshServerContext serverContext = createServerContext(daemonContext, sc);
			return serverContext;
		}
	}

	@Override
	protected ProtocolContextFactory<?> getDefaultContextFactory() {
		return defaultContextFactory;
	}


}
