
package com.sshtools.client;

import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Implements public key authentication, taking a com.sshtools.publickey.SshKeyPair object as the source private key.
 */
public class KeyPairAuthenticator extends PublicKeyAuthenticator {

	SshKeyPair pair;

	public KeyPairAuthenticator(SshKeyPair pair) {
		this.pair = pair;
	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) {
		
		try {
			setKeyPair(pair);
			super.authenticate(transport, username);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
