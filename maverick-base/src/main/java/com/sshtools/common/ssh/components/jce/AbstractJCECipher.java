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
/* HEADER */
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.components.SshCipher;

/**
 * <p>An abstract base class for defining SSH ciphers which use a JCE
 * provider instead of the internal Maverick Crypto provider.</p>
 *
 * @author Lee David Painter
 */
public class AbstractJCECipher extends SshCipher {

  Cipher cipher;
  String spec;
  String keyspec;
  int keylength;

  /**
   *
   * @param spec the value passed into Cipher.getInstance() that specifies the
   * specification of the cipher; for example "Blowfish/CBC/NoPadding"
   * @param keyspec the value passed into the constructor of SecretKeySpec.
   * @param keylength int the length in bytes of the key
   */
  public AbstractJCECipher(String spec, String keyspec, int keylength, String algorithm) throws IOException {
    super(algorithm);
    this.spec = spec;
    this.keylength = keylength;
    this.keyspec = keyspec;
    
    try {
    	cipher = createCipher(spec);
    } catch (NoSuchPaddingException nspe) {
        throw new IOException("Padding type not supported");
    } catch (NoSuchAlgorithmException nsae) {
       throw new IOException("Algorithm not supported:"+spec);
    } 
  
    if(cipher==null) {
        throw new IOException("Failed to create cipher engine for "
                                + spec);
    }
  }
  
  public String getProviderName() {
	  return cipher.getProvider().getName();
  }
  
  protected Cipher createCipher(String spec) throws NoSuchAlgorithmException, NoSuchPaddingException {
	  return JCEProvider.getProviderForAlgorithm(spec)==null ?
	          	Cipher.getInstance(spec)
	          	: Cipher.getInstance(spec, JCEProvider.getProviderForAlgorithm(spec));
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
			cipher.update(buf, start, len, output, off);
		} catch (ShortBufferException e) {
			throw new IOException(e.getMessage(), e);
		}
    }
  }
  
  public String getProvider() {
	  if(cipher==null) {
		  return null;
	  }
	  return cipher.getProvider().getName();
  }
  
  public int getKeyLength() {
	  return keylength;
  }

  public void init(int mode, byte[] iv, byte[] keydata) throws java.io.IOException {

      try {
          // Create a byte key
          byte[] actualKey = new byte[keylength];
          System.arraycopy(keydata, 0, actualKey, 0, actualKey.length);

          SecretKeySpec kspec = new SecretKeySpec(actualKey, keyspec);

          // Create the cipher according to its algorithm
          cipher.init(((mode == ENCRYPT_MODE) ? Cipher.ENCRYPT_MODE
                       : Cipher.DECRYPT_MODE),
                      kspec, new IvParameterSpec(iv, 0, getBlockSize()));
      } catch (InvalidKeyException ike) {
          throw new IOException("Invalid encryption key");
      } catch (InvalidAlgorithmParameterException ape) {
          throw new IOException("Invalid algorithm parameter");
      }
  }

  public int getBlockSize() {
    return cipher.getBlockSize();
  }

}
