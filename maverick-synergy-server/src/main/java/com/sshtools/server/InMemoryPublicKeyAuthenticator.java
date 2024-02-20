package com.sshtools.server;

/*-
 * #%L
 * Server API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
