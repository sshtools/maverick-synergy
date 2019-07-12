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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.auth.AbstractPublicKeyAuthenticationProvider;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

public class InMemoryPublicKeyAuthenticator extends AbstractPublicKeyAuthenticationProvider {

	Map<String,SshPublicKey> authorizedKeys = new HashMap<>();
	
	public InMemoryPublicKeyAuthenticator() {
	
	}

	public InMemoryPublicKeyAuthenticator addAuthorizedKey(String username, SshPublicKey key) {
		authorizedKeys.put(username, key);
		return this;
	}
	
	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		return key.equals(authorizedKeys.get(con.getUsername()));
	}

}
