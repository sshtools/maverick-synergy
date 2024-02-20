package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.components.SshPrivateKey;

public interface SshEd25519PrivateKey extends SshPrivateKey {

	byte[] getSeed();

}
