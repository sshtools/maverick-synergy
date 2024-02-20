package com.sshtools.client;

/*-
 * #%L
 * Client API
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication, taking a com.sshtools.publickey.SshKeyPair object as the source private key.
 */
public class KeyPairAuthenticator extends PublicKeyAuthenticator {

	List<SshKeyPair> pairs;
	SshKeyPair authenticatingPair;
	
	public KeyPairAuthenticator(SshKeyPair pair) {
		this.pairs = new ArrayList<>(Arrays.asList(pair));
	}

	public KeyPairAuthenticator(SshKeyPair... identities) {
		this.pairs = new ArrayList<>(Arrays.asList(identities));
	}

	@Override
	protected SshPublicKey getNextKey() {
		return authenticatingPair.getPublicKey();
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() {
		return authenticatingPair;
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		if(!pairs.isEmpty()) {
			this.authenticatingPair = pairs.remove(0);
			return true;
		}
		return false;
	}

}
