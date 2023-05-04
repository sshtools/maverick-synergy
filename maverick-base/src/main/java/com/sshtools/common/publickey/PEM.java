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

package com.sshtools.common.publickey;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;

class PEM {
 
  public final static String DSA_PRIVATE_KEY = "DSA PRIVATE KEY";
  public final static String RSA_PRIVATE_KEY = "RSA PRIVATE KEY"; 
  public final static String EC_PRIVATE_KEY = "EC PRIVATE KEY";
  public final static String OPENSSH_PRIVATE_KEY = "OPENSSH PRIVATE KEY";

  /**  */
  protected final static String PEM_BOUNDARY = "-----";

  /**  */
  protected final static String PEM_BEGIN = PEM_BOUNDARY + "BEGIN ";

  /**  */
  protected final static String PEM_END = PEM_BOUNDARY + "END ";

  /**  */
  protected final static int MAX_LINE_LENGTH = 75;

  /**  */
  protected final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
  private final static int MD5_HASH_BYTES = 0x10;

  /**
   *
   *
   * @param passphrase
   * @param iv
   * @param keySize
   *
   * @return byte[]
   */
  protected static byte[] getKeyFromPassphrase(String passphrase,
                                               byte[] iv, int keySize)
     throws
     IOException {
    try {
		byte[] passphraseBytes;

		try {
		  passphraseBytes = passphrase == null ? new byte[0] :
		     passphrase.getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException e) {
		  throw new IOException(
		     "Mandatory US-ASCII character encoding is not supported by the VM");
		}

		/*
		   hash is MD5
		   h(0) <- hash(passphrase, iv);
		   h(n) <- hash(h(n-1), passphrase, iv);
		   key <- (h(0),...,h(n))[0,..,key.length];
		 */
		Digest hash = (Digest) ComponentManager.getInstance().supportedDigests().getInstance("MD5");

		byte[] key = new byte[keySize];

		int hashesSize = keySize & 0xfffffff0;

		if((keySize & 0xf) != 0) {
		  hashesSize += MD5_HASH_BYTES;
		}

		byte[] hashes = new byte[hashesSize];

		byte[] previous;

		for(int index = 0; (index + MD5_HASH_BYTES) <= hashes.length;
		    hash.putBytes(previous, 0, previous.length)) {
		  hash.putBytes(passphraseBytes, 0, passphraseBytes.length);
		  hash.putBytes(iv, 0, 8);
		  previous = hash.doFinal();
		  System.arraycopy(previous, 0, hashes, index, previous.length);
		  index += previous.length;
		}

		System.arraycopy(hashes, 0, key, 0, key.length);

		return key;
	} catch (SshException e) {
		throw new SshIOException(e);
	}
  }
}
