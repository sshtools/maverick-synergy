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

import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.auth.MutualKeyAuthenticatonStore;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MutualCallbackAuthenticationProvider implements Authenticator {

	public static final String MUTUAL_KEY_AUTHENTICATION = "mutual-key-auth@sshtools.com";
	MutualKeyAuthenticatonStore authenticationStore;
	
	public MutualCallbackAuthenticationProvider(MutualKeyAuthenticatonStore authenticationStore) {
		this.authenticationStore = authenticationStore;
	}
	
	public SshKeyPair getLocalPrivateKey(SshConnection con) {
		return authenticationStore.getPrivateKey(con);
	}
	
	public SshPublicKey getRemotePublicKey(SshConnection con) {
		return authenticationStore.getPublicKey(con);
	}
}
