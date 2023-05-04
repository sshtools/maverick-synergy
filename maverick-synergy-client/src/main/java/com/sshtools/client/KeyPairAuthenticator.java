/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.client;

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
