package com.sshtools.common.auth;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface MutualKeyAuthenticatonStore {

	SshKeyPair getPrivateKey(SshConnection con);

	SshPublicKey getPublicKey(SshConnection con);

}
