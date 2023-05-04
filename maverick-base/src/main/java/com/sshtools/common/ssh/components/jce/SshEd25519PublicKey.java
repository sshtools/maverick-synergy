package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.components.SshPublicKey;

public interface SshEd25519PublicKey extends SshPublicKey {

	byte[] getA();

}