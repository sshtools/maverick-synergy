package com.sshtools.server.callback;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.TransportProtocolClient;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.LicenseException;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.TransportProtocolServer;

class TransportProtocolSwitchingClient extends TransportProtocolClient {

	ProtocolContextFactory<SshServerContext> serverFactory;
	String callbackIdentifier;
	ConnectRequestFuture connectFuture;
	
	public TransportProtocolSwitchingClient(SshClientContext sshContext, 
			String callbackIdentifier,
			ProtocolContextFactory<SshServerContext> serverFactory,
			ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext, connectFuture);
		this.serverFactory = serverFactory;
		this.callbackIdentifier = callbackIdentifier;
		this.connectFuture = connectFuture;
	}

	@Override
	protected void onRemoteIdentificationReceived(String remoteIdentification) {
		
		if(!remoteIdentification.startsWith("SSH-2.0-" + callbackIdentifier)) {
			try {
				SshServerContext context = serverFactory.createContext(sshContext.getDaemonContext(), getSocketConnection().getSocketChannel());
				TransportProtocolServer engine = (TransportProtocolServer) context.createEngine(connectFuture);
				transferState(engine);
				getSocketConnection().setProtocolEngine(engine);
			} catch (Exception e) {
				disconnect(PROTOCOL_ERROR, "Failed to switch SSH role");
			}
		}
	}

	
}
