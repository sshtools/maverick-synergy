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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh.components;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.KeyGenerator;
import com.sshtools.common.ssh.SshException;

public class Ed25519KeyGenerator implements KeyGenerator {

	@Override
	public SshKeyPair generateKey(int bits) throws SshException {
		
		try {

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EdDSA", "EdDSA");
			KeyPair kp = keyGen.generateKeyPair();

			SshKeyPair pair = new SshKeyPair();
			pair.setPrivateKey(new SshEd25519PrivateKey(kp.getPrivate()));
			pair.setPublicKey(new SshEd25519PublicKey(kp.getPublic()));
			
			return pair;
		} catch (NoSuchAlgorithmException
				| NoSuchProviderException e) {
			if(Log.isErrorEnabled()) {
				Log.error("ed25519 keys are not supported with the current configuration", e);
			}
			throw new SshException("ed25519 keys are not supported with the current configuration", SshException.BAD_API_USAGE);
		}
	}

}
