package com.sshtools.server.callback;

import java.io.IOException;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.common.nio.ProtocolEngine;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.server.SshServerContext;

class SwitchingSshContext extends SshClientContext {

	ProtocolContextFactory<SshServerContext> serverFactory;
	String clientIdentifier;
	
	SwitchingSshContext(SshEngine engine, 
			String clientIdentifier,
			ProtocolContextFactory<SshServerContext> serverFactory) throws IOException {
		super(engine);
		this.clientIdentifier = clientIdentifier;
		this.serverFactory = serverFactory;
	}

	@Override
	public ProtocolEngine createEngine(ConnectRequestFuture connectFuture) throws IOException {
		return new TransportProtocolSwitchingClient(this, clientIdentifier, serverFactory, connectFuture);
	}

	
}
