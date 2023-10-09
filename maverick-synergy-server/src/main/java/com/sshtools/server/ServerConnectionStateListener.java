package com.sshtools.server;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.ConnectionStateListener;

public interface ServerConnectionStateListener extends ConnectionStateListener {

	default public void authenticationComplete(SshConnection con) {
		
	}
}
