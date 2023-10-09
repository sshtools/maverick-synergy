package com.sshtools.synergy.ssh;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.AbstractRequestFuture;

public class AuthenticatedFuture extends AbstractRequestFuture {

	TransportProtocol<?> transport;
	public AuthenticatedFuture(TransportProtocol<?> transport) {
		this.transport = transport;
	}
	public void authenticated(boolean success) {
		done(success);
	}

	@Override
	public synchronized void done(boolean success) {
		if(Log.isDebugEnabled()) {
			Log.debug("Authenticated connection {}", transport.getUUID());
		}
		super.done(success);
	}


}
