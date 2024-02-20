package com.sshtools.callback.client;

/*-
 * #%L
 * Callback Client API
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
import java.util.Set;

import com.sshtools.common.auth.AbstractPublicKeyAuthenticationProvider;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

public class CallbackServerAuthentication extends AbstractPublicKeyAuthenticationProvider {

	Set<SshPublicKey> serverKeys;
	
	CallbackServerAuthentication(Set<SshPublicKey> serverKeys) throws IOException {
		
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

}
