package com.sshtools.callback.client;

import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.auth.MutualKeyAuthenticatonStore;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MutualCallbackAuthenticationProvider implements Authenticator {

	public static final String MUTUAL_KEY_AUTHENTICATION = "mutual-key-auth@sshtools.com";
	MutualKeyAuthenticatonStore authenticationStore;
	
	public MutualCallbackAuthenticationProvider(MutualKeyAuthenticatonStore authenticationStore) {
		this.authenticationStore = authenticationStore;
	}
	
	public SshKeyPair getLocalPrivateKey(String username) {
		return authenticationStore.getPrivateKey(username);
	}
	
	public SshPublicKey getRemotePublicKey(String username) {
		return authenticationStore.getPublicKey(username);
	}
}
