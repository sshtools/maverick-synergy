package com.sshtools.client;

import java.util.List;
import java.util.Set;

import com.sshtools.common.ssh.Connection;

public class ClientStateAdapter implements ClientStateListener {

	@Override
	public void connected(Connection<SshClientContext> con) {

	}

	@Override
	public void disconnected(Connection<SshClientContext> con) {

	}

	@Override
	public void authenticate(Connection<SshClientContext> con, 
			Set<String> supportedAuths, 
			boolean moreRequired,
			List<ClientAuthenticator> authsToTry) {

	}

}
