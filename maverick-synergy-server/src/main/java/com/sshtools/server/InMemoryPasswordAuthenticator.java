/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
