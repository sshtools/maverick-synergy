package com.sshtools.common.auth;

import com.sshtools.common.ssh.SshConnection;

public interface KeyboardInteractiveAuthenticationProvider extends
		Authenticator {

	KeyboardInteractiveProvider createInstance(SshConnection con);

}