package com.sshtools.common.auth;


import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class DefaultPublicKeyAuthenticationVerifier implements PublicKeyAuthenticationVerifier {

	@Override
	public boolean verifySignature(SshPublicKey key, byte[] signature, byte[] data) throws SshException {
		return key.verifySignature(signature, data);
	}

}
