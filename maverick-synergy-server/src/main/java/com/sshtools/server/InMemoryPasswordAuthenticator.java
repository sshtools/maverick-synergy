
package com.sshtools.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.auth.PasswordAuthenticationProvider;
import com.sshtools.common.auth.PasswordChangeException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.Arrays;

public class InMemoryPasswordAuthenticator extends PasswordAuthenticationProvider {

	Map<String,char[]> users = new HashMap<>();
	
	public InMemoryPasswordAuthenticator addUser(String name, char[] password) {
		users.put(name, password);
		return this;
	}
	
	@Override
	public boolean verifyPassword(SshConnection con, String username, String password)
			throws PasswordChangeException, IOException {
		char[] pwd = users.get(username);
		if(Objects.isNull(pwd)) {
			return false;
		}
		return Arrays.areEqual(pwd, password.toCharArray());
	}

	@Override
	public boolean changePassword(SshConnection con, String username, String oldpassword, String newpassword)
			throws PasswordChangeException, IOException {
		return false;
	}

}
