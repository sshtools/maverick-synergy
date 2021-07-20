package com.sshtools.common.auth;

import java.io.IOException;

import com.sshtools.common.ssh.SshConnection;

public class KeyboardInteractiveAuthenticator implements KeyboardInteractiveAuthenticationProvider {

	Class<? extends KeyboardInteractiveProvider> clz;
	
	public KeyboardInteractiveAuthenticator(Class<? extends KeyboardInteractiveProvider> clz) {
		this.clz = clz;
	}
	
	@Override
	public KeyboardInteractiveProvider createInstance(SshConnection con) throws IOException {
		try {
			return clz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
