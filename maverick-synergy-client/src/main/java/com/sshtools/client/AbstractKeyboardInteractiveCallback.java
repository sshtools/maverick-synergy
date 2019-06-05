package com.sshtools.client;

import com.sshtools.common.ssh.Connection;

public abstract class AbstractKeyboardInteractiveCallback implements KeyboardInteractiveCallback {

	protected Connection<SshClientContext> connection;
	
	@Override
	public void init(Connection<SshClientContext> connection) {
		this.connection = connection;
	}
}
