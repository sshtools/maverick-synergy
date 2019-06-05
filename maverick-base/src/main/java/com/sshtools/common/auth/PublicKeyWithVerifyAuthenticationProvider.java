package com.sshtools.common.auth;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface PublicKeyWithVerifyAuthenticationProvider extends PublicKeyAuthenticationProvider {

	boolean checkKey(SshPublicKey key, SshConnection con);
}
