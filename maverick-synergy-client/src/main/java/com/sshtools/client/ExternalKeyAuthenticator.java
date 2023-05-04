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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication taking a separately loaded SshKeyPair as the private key for authentication.
 */
public class ExternalKeyAuthenticator extends PublicKeyAuthenticator {

	SignatureGenerator signatureGenerator;
	List<SshPublicKey> publickeys;
	SshPublicKey authenticatingKey = null;

	public ExternalKeyAuthenticator(SignatureGenerator signatureGenerator) throws IOException {
		this.signatureGenerator = signatureGenerator;
		this.publickeys = new ArrayList<>(signatureGenerator.getPublicKeys());
	}

	@Override
	protected SignatureGenerator getSignatureGenerator() throws IOException, InvalidPassphraseException {
		return signatureGenerator;
	}
	
	@Override
	protected SshPublicKey getNextKey() throws IOException {
		return authenticatingKey;
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		if(!publickeys.isEmpty()) {
			authenticatingKey = publickeys.remove(0);
			if(Log.isDebugEnabled()) {
				try {
					Log.debug("Using key {}", SshKeyUtils.getOpenSSHFormattedKey(authenticatingKey));
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
			return true;
		}
		return false;
	}


	

}
