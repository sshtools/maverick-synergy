package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.components.SshPrivateKey;

public interface SshEd448PrivateKey extends SshPrivateKey {

	byte[] getSeed();

}
