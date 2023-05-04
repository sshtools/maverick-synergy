package com.sshtools.common.auth;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface PublicKeyAuthenticationVerifier {

	boolean verifySignature(SshPublicKey key, byte[] signature, byte[] data) throws SshException;

}
