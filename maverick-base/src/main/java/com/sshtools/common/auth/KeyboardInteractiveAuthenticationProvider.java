package com.sshtools.common.auth;

import java.io.IOException;

import com.sshtools.common.ssh.SshConnection;

public interface KeyboardInteractiveAuthenticationProvider extends
		Authenticator {

	KeyboardInteractiveProvider createInstance(SshConnection con) throws IOException;

}
