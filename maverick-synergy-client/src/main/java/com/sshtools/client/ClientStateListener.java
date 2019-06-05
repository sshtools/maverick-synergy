package com.sshtools.client;

import java.util.List;
import java.util.Set;

import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionStateListener;

public interface ClientStateListener extends ConnectionStateListener<SshClientContext>{

	public void authenticate(Connection<SshClientContext> con, Set<String> supportedAuths, boolean moreRequired, List<ClientAuthenticator> authsToTry);
}
