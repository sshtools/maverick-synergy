package com.sshtools.common.auth;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface MutualKeyAuthenticatonStore {

	SshKeyPair getPrivateKey(String username);

	SshPublicKey getPublicKey(String username);

}