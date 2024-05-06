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
			public void disconnected(SshConnection con) {
				if(con.getRemoteIdentification().substring(8).startsWith(callbackIdentifier)) {
					Log.info("Callback client {} disconnected", con.getUsername());
					callbacks.unregisterCallbackClient(con.getUUID());
				}
				
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
							Log.info("Callback client {} registered with memo {}", connection.getUUID(), memo);
						}
						callbacks.registerCallbackClient(connection.getConnection(), memo);
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
