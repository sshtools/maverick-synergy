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
package com.sshtools.callback.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import com.sshtools.common.auth.PublicKeyAuthenticationProvider;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class ServerPublicKeyAuthentication implements PublicKeyAuthenticationProvider {

	
	
	Set<SshPublicKey> serverKeys;
	
	ServerPublicKeyAuthentication(Set<SshPublicKey> serverKeys) throws IOException {
		
		this.serverKeys = serverKeys;
		
		if(serverKeys.isEmpty()) {
			throw new IOException("There are no keys available to authenticate the server!");
		}
	}
	
	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		
		for(SshPublicKey serverKey : serverKeys) {
			if(key.equals(serverKey)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<SshPublicKeyFile> getKeys(SshConnection con)
			throws PermissionDeniedException, IOException {
		throw new PermissionDeniedException("Unsupported");
	}

	@Override
	public void remove(SshPublicKey key, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new PermissionDeniedException("Unsupported");
	}

	@Override
	public void add(SshPublicKey key, String comment, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new PermissionDeniedException("Unsupported");
	}

}
