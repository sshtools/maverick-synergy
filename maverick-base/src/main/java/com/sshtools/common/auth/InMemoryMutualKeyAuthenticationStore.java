package com.sshtools.common.auth;

/*-
 * #%L
 * Base API
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

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class InMemoryMutualKeyAuthenticationStore implements MutualKeyAuthenticatonStore {

	Map<String,SshKeyPair> privateKeys = new HashMap<>();
	Map<String,SshPublicKey> publicKeys = new HashMap<>();
	
	
	@Override
	public SshKeyPair getPrivateKey(SshConnection con) {
		return privateKeys.get(con.getUsername());
	}
	
	@Override
	public SshPublicKey getPublicKey(SshConnection con) {
		return publicKeys.get(con.getUsername());
	}

	public InMemoryMutualKeyAuthenticationStore addKey(String username, SshKeyPair privateKey, SshPublicKey publicKey) {
		privateKeys.put(username, privateKey);
		publicKeys.put(username, publicKey);
		return this;
	}
	
}
