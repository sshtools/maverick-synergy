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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;

public class AES128Gcm extends AbstractJCECipher {

	byte[] key;
	byte[] nonce;
	int mode;
	public AES128Gcm() throws IOException {
		super(JCEAlgorithms.JCE_AESGCMNOPADDING, "AES", 16, "aes128-gcm@openssh.com", SecurityLevel.PARANOID, 5000);
	}

	public void init(int mode, byte[] iv, byte[] keydata) throws java.io.IOException {

		  this.mode = mode;
	      try {
	          // Create a byte key
	          key = new byte[keylength];
	          System.arraycopy(keydata, 0, key, 0, key.length);

	          SecretKeySpec kspec = new SecretKeySpec(key, keyspec);

	          nonce = new byte[12];
	          System.arraycopy(iv, 0, nonce, 0, nonce.length);
	          GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
	          cipher.init(((mode == ENCRYPT_MODE) ? Cipher.ENCRYPT_MODE
                      : Cipher.DECRYPT_MODE), kspec, spec);
	          
	      } catch (InvalidKeyException ike) {
	          throw new IOException("Invalid encryption key");
	      } catch (InvalidAlgorithmParameterException ape) {
	          throw new IOException("Invalid algorithm parameter");
	      }
	  }
	
	public void transform(byte[] buf, int start, byte[] output, int off, int len) throws java.io.IOException {
		if(len > 0) {
	    
			if(buf.length-start < len) {
				throw new IllegalStateException("Input buffer of " + buf.length + " bytes is too small for requested transform length " + len);
			}
			if(output.length-off < len) {
				throw new IllegalStateException("Output buffer of " + output.length + " bytes is too small for requested transform length " + len);
			}
			
			try {
				cipher = createCipher(JCEAlgorithms.JCE_AESGCMNOPADDING);
				
				SecretKeySpec kspec = new SecretKeySpec(key, keyspec);
				GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
		        cipher.init(((mode == ENCRYPT_MODE) ? Cipher.ENCRYPT_MODE
	                    : Cipher.DECRYPT_MODE), kspec, spec);
		          
				cipher.updateAAD(buf, start, 4);
				System.arraycopy(buf, start, output, off, 4);

				byte[] tmp = cipher.doFinal(buf, start+4, len-4);
				System.arraycopy(tmp, 0, output, off+4, tmp.length);
				
				incrementIv();
			} catch (IllegalBlockSizeException
					| BadPaddingException
					| InvalidKeyException
					| InvalidAlgorithmParameterException
					| NoSuchAlgorithmException
					| NoSuchPaddingException e) {
				throw new IOException(e.getMessage(), e);
			}
	    }
	  }
	
	private void incrementIv() {
		for(int i = 4 + 7; i >= 4; i--) {
			nonce[i]++;
			if(nonce[i] != 0) {
				break;
			}
		}
	}
	
	@Override
	public boolean isMAC() {
		return true;
	}
	
	@Override
	public int getMacLength() {
		return 16;
	}
}
