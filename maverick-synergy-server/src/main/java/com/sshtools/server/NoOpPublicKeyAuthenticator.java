
package com.sshtools.server;

import java.io.IOException;

import com.sshtools.common.auth.PublicKeyAuthenticationAdapter;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

public class NoOpPublicKeyAuthenticator extends PublicKeyAuthenticationAdapter {

	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		return false;
	}

	@Override
	public boolean checkKey(SshPublicKey key, SshConnection con) throws IOException {
		return false;
	}

}
