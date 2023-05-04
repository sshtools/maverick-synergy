package com.sshtools.client;

import java.util.Set;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.ConnectionStateListener;

public interface ClientStateListener extends ConnectionStateListener {

	default public void authenticate(AuthenticationProtocolClient auth, 
			SshConnection con, Set<String> supportedAuths, 
				boolean moreRequired) { 
	}

	default public void authenticationStarted(AuthenticationProtocolClient authenticationProtocolClient,
			SshConnection connection) {
		
	}
}
