
package com.sshtools.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.SshEngineContext;

public class SshServer extends AbstractSshServer implements ProtocolContextFactory<SshServerContext> {

	public SshServer() {
		JCEComponentManager.getDefaultInstance();
	}
	
	protected ProtocolContextFactory<?> getDefaultContextFactory() {
		return this;
	}

	public SshServer(int port) throws UnknownHostException {
		this("::", port);
	}
	
	public SshServer(String addressToBind, int port) throws UnknownHostException {
		this(InetAddress.getByName(addressToBind), port);
	}
	
	public SshServer(InetAddress addressToBind, int port) {
		this.addressToBind = addressToBind;
		this.port = port;
		JCEComponentManager.getDefaultInstance();
	}

	@Override
	public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
			throws IOException, SshException {
		return createServerContext(daemonContext, sc);
	}
	
	
}
