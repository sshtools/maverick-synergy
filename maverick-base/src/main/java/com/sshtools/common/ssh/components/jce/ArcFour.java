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
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class ArcFour extends AbstractJCECipher {
	
	public ArcFour() throws IOException {
		super(JCEAlgorithms.JCE_ARCFOUR, "ARCFOUR", 16, "arcfour");	
	}

	public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
	      try {

	          cipher = JCEProvider.getProviderForAlgorithm(spec)==null ?
	              	Cipher.getInstance(spec)
	              	: Cipher.getInstance(spec, JCEProvider.getProviderForAlgorithm(spec));

	          if(cipher==null) {
	              throw new IOException("Failed to create cipher engine for "
	                                    + spec);
	          }

	          // Create a byte key
	          byte[] actualKey = new byte[keylength];
	          System.arraycopy(keydata, 0, actualKey, 0, actualKey.length);

	          SecretKeySpec kspec = new SecretKeySpec(actualKey, keyspec);

	          // Create the cipher according to its algorithm
	          cipher.init(((mode == ENCRYPT_MODE) ? Cipher.ENCRYPT_MODE
	                       : Cipher.DECRYPT_MODE),
	                      kspec);
	          
	      } catch (NoSuchPaddingException nspe) {
	          throw new IOException("Padding type not supported");
	      } catch (NoSuchAlgorithmException nsae) {
	          throw new IOException("Algorithm not supported:"+spec);
	      } catch (InvalidKeyException ike) {
	          throw new IOException("Invalid encryption key");
	      } 
	}

	public int getBlockSize() {
		return 8;
	}
}
