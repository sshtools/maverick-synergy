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
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;

import com.sshtools.common.ssh.components.SshPrivateKey;

public abstract class Ssh2BaseRsaPrivateKey extends Ssh2BaseJCEPrivateKey implements SshPrivateKey {


    public Ssh2BaseRsaPrivateKey(PrivateKey prv) {
    	super(prv);
    }

    public Ssh2BaseRsaPrivateKey(PrivateKey prv, Provider customProvider) {
        super(prv, customProvider);
    }

    protected byte[] doSign(byte[] data, String signingAlgorithm) throws IOException {
    	
    	Signature l_sig;
    	
		switch(signingAlgorithm) {
		case "rsa-sha2-256":
			try {
				l_sig = getJCESignature(JCEAlgorithms.JCE_SHA256WithRSA);
				break;
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		case "rsa-sha2-512":
			try {
				l_sig = getJCESignature(JCEAlgorithms.JCE_SHA512WithRSA);
				break;
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		default:
			try {
				l_sig = getJCESignature(JCEAlgorithms.JCE_SHA1WithRSA);
				break;
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		}

		try {
			l_sig.initSign(prv);
			l_sig.update(data);
			return l_sig.sign();
		} catch (SignatureException | InvalidKeyException e) {
			throw new IOException(e.getMessage(), e);
		}
    }

	
}



