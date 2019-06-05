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

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;

import com.sshtools.common.ssh.components.SshPrivateKey;

public abstract class Ssh2BaseJCEPrivateKey implements SshPrivateKey {

	protected PrivateKey prv;
	protected Provider customProvider;
	
	public Ssh2BaseJCEPrivateKey(PrivateKey prv) {
		this.prv = prv;
	}
	
	public Ssh2BaseJCEPrivateKey(PrivateKey prv, Provider customProvider) {
		this.prv = prv;
		this.customProvider = customProvider;
	}
	
	public PrivateKey getJCEPrivateKey() {
		return prv;
	}
	
	protected Signature getJCESignature(String algorithm) throws NoSuchAlgorithmException {
		
		Signature  sig = null;
		if(customProvider!=null) {
			try {
				sig = Signature.getInstance(algorithm, customProvider);
			} catch(NoSuchAlgorithmException e) {
			}
		}
		
		if(sig==null) {
			sig = JCEProvider.getProviderForAlgorithm(algorithm)== null ? 
						Signature.getInstance(algorithm)
					:   Signature.getInstance(algorithm, 
						JCEProvider.getProviderForAlgorithm(algorithm));
		}
		return sig;
	}
}
