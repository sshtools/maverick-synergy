package com.sshtools.callback.client;

import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.auth.MutualKeyAuthenticationStore;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MutualCallbackAuthenticationProvider implements Authenticator {

	MutualKeyAuthenticationStore authenticationStore;
	
	public MutualCallbackAuthenticationProvider(MutualKeyAuthenticationStore authenticationStore) {
		this.authenticationStore = authenticationStore;
	}
	
	public SshKeyPair getLocalPrivateKey(String username) {
		return authenticationStore.getPrivateKey(username);
	}
	
	public SshPublicKey getRemotePublicKey(String username) {
		return authenticationStore.getPublicKey(username);
	}
}
