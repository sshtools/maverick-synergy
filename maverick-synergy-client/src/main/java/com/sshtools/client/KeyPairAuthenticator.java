package com.sshtools.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication, taking a com.sshtools.publickey.SshKeyPair object as the source private key.
 */
public class KeyPairAuthenticator extends PublicKeyAuthenticator {

	List<SshKeyPair> pairs;
	SshKeyPair authenticatingPair;
	
	public KeyPairAuthenticator(SshKeyPair pair) {
		this.pairs = new ArrayList<>(Arrays.asList(pair));
	}

	public KeyPairAuthenticator(SshKeyPair... identities) {
		this.pairs = new ArrayList<>(Arrays.asList(identities));
	}

	@Override
	protected SshPublicKey getNextKey() {
		return authenticatingPair.getPublicKey();
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() {
		return authenticatingPair;
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		if(!pairs.isEmpty()) {
			this.authenticatingPair = pairs.remove(0);
			return true;
		}
		return false;
	}

}
