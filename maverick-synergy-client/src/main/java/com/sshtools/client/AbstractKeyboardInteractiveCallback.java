
package com.sshtools.client;

import com.sshtools.common.ssh.SshConnection;

public abstract class AbstractKeyboardInteractiveCallback implements KeyboardInteractiveCallback {

	protected SshConnection connection;
	
	@Override
	public void init(SshConnection connection) {
		this.connection = connection;
	}
}
