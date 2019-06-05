package com.sshtools.common.publickey;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;

public interface KeyGenerator {

	SshKeyPair generateKey(int bits) throws SshException;
}
