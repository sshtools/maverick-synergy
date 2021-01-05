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
package com.sshtools.common.ssh.components.jce;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshSecureRandomGenerator;

/**
 * Secure random number generator implementation for JCE provider. 
 */
public class SecureRND implements SshSecureRandomGenerator {

	SecureRandom rnd;
	
	public SecureRND() throws NoSuchAlgorithmException {
		rnd = JCEProvider.getSecureRandom();
	}
	
	public void nextBytes(byte[] bytes) {
		rnd.nextBytes(bytes);
	}

	public void nextBytes(byte[] bytes, int off, int len) throws SshException {
		
		try {
			byte[] tmp = new byte[len];
			rnd.nextBytes(tmp);
			System.arraycopy(tmp, 0, bytes, off, len);
		} catch(ArrayIndexOutOfBoundsException ex){
			throw new SshException("ArrayIndexOutOfBoundsException: Index " + off + " on actual array length " + bytes.length + " with len=" + len, SshException.INTERNAL_ERROR);
		}
	}

	public int nextInt() {
		return rnd.nextInt();
	}

}
