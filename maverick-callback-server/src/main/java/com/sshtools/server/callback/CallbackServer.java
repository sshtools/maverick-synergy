package com.sshtools.server.callback;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.SshServerContext;

/**
 * An abstract server that provides a callback facility, listening on a port and acting as a client to 
 * any callback clients that connect to it. The callback client similarly acts as a server allowing 
 * this server to perform operations on the remote client.
 * 
 * The server also has the facility to act as a normal server. Switching modes depending on the 
 * client identifier provided by the SSH client.
 */
public abstract class CallbackServer extends SshEngine {

	String callbackIdentifier;
	
	protected CallbackServer(String callbackIdentifier) throws IOException {
		this.callbackIdentifier = callbackIdentifier;
	}
	
	public void addInterface(InetAddress addressToBind, int portToBind) throws IOException {
		getContext().addListeningInterface(addressToBind, portToBind, new ClientContextFactory(), true);
	}
	
	protected abstract void configureClientContext(SshClientContext clientContext);

	protected abstract SshServerContext createServerContext(SshEngineContext daemonContext, 
			SocketChannel sc);
	

	class ClientContextFactory implements ProtocolContextFactory<SshClientContext> {

		@Override
		public SshClientContext createContext(SshEngineContext daemonContext, SocketChannel sc)
				throws IOException, SshException {
			SshClientContext clientContext = new SwitchingSshContext(CallbackServer.this, callbackIdentifier, new ServerContextFactory());
			configureClientContext(clientContext);
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
}
