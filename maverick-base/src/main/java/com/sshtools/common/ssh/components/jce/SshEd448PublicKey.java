package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.components.SshPublicKey;

public interface SshEd448PublicKey extends SshPublicKey {

	byte[] getA();

}