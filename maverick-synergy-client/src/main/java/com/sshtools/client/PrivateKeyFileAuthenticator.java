/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.File;
import java.io.FileInputStream;

import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Implements public key authentication, taking a java.io.File object as the source private key.
 */
public class PrivateKeyFileAuthenticator extends PublicKeyAuthenticator {

	File keyfile;
	String passphrase;
	
	public PrivateKeyFileAuthenticator(File keyfile, String passphrase) {
		this.keyfile = keyfile;
		this.passphrase = passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File keyfile) {
		this.keyfile = keyfile;
	}
	
	public String getPassphrase() {
		return passphrase;
	}
	
	@Override
	public void authenticate(TransportProtocolClient transport, String username) {
		
		try {
			SshPrivateKeyFile privateKeyFile = SshPrivateKeyFileFactory.parse(new FileInputStream(keyfile));
			SshKeyPair pair;
			if(privateKeyFile.isPassphraseProtected()) {
				pair = privateKeyFile.toKeyPair(getPassphrase());
			} else {
				pair = privateKeyFile.toKeyPair(null);
			}
			setKeyPair(pair);
			super.authenticate(transport, username);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
