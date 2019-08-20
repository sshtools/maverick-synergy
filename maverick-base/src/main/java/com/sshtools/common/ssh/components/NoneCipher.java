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

import java.io.IOException;

/**
 * <p>This special cipher implementation provides an unencrypted connection. This
 * is not enabled by default and should be used with caution. To enable 
 * and use the cipher you should add the following code before you connect 
 * your SSH client.</p> 
 * 
 * <blockquote><pre>
 * SshConnector con = SshConnector.getInstance();
 * Ssh2Context ssh2Context = (Ssh2Context) con.getContext(SshConnector.SSH2);
 * ssh2Context.supportedCiphers().add("none", NoneCipher.class);
 * ssh2Context.setPreferredCipherCS("none");
 * ssh2Context.setPreferredCipherSC("none");
 * </pre><blockquote>  
 * 
 * 
 * @author Lee David Painter
 *
 */
public class NoneCipher extends SshCipher {
    public NoneCipher() {
        super("none");
    }

    /**
     * Get the cipher block size.
     *
     * @return the block size in bytes.
     * @todo Implement this com.maverick.ssh.cipher.SshCipher method
     */
    public int getBlockSize() {
        return 8;
    }
    
    public int getKeyLength() {
    	return 8;
    }

    /**
     * Initialize the cipher with up to 40 bytes of iv and key data.
     *
     * @param mode the mode to operate
     * @param iv the initiaization vector
     * @param keydata the key data
     * @throws IOException
     * @todo Implement this com.maverick.ssh.cipher.SshCipher method
     */
    public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
    }

    /**
     * Transform the byte array according to the cipher mode; it is legal for
     * the source and destination arrays to reference the same physical array
     * so care should be taken in the transformation process to safeguard
     * this rule.
     *
     * @param src byte[]
     * @param start int
     * @param dest byte[]
     * @param offset int
     * @param len int
     * @throws IOException
     * @todo Implement this com.maverick.ssh.cipher.SshCipher method
     */
    public void transform(byte[] src, int start, byte[] dest, int offset,
                          int len) throws IOException {
    }

	@Override
	public String getProviderName() {
		return "None";
	}
}
