package com.sshtools.common.ssh.components;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SecurityLevel;

public interface SshCipher extends  SshComponent, SecureComponent {
	
	SecurityLevel getSecurityLevel();

	int getPriority();

	String getAlgorithm();

	/**
	   * Encryption mode.
	   */
	int ENCRYPT_MODE = 0;
	/**
	   * Decryption mode.
	   */
	int DECRYPT_MODE = 1;

	/**
	   * Get the cipher block size.
	   *
	   * @return the block size in bytes.
	   */
	int getBlockSize();

	/**
	   * Return the key length required
	   * @return
	   */
	int getKeyLength();

	/**
	   * Initialize the cipher with up to 40 bytes of iv and key data. Each implementation
	   * should take as much data from the initialization as it needs ignoring any data
	   * that it does not require.
	   *
	   * @param mode     the mode to operate
	   * @param iv       the initiaization vector
	   * @param keydata  the key data
	   * @throws IOException
	   */
	void init(int mode, byte[] iv, byte[] keydata) throws IOException;

	/**
	   * Transform the byte array according to the cipher mode.
	   * @param data
	   * @throws IOException
	   */
	void transform(byte[] data) throws IOException;

	/**
	   * Transform the byte array according to the cipher mode; it is legal for the
	   * source and destination arrays to reference the same physical array so
	   * care should be taken in the transformation process to safeguard this rule.
	   *
	   * @param src
	   * @param start
	   * @param dest
	   * @param offset
	   * @param len
	   * @throws IOException
	   */
	void transform(byte[] src, int start, byte[] dest, int offset, int len) throws IOException;

	boolean isMAC();

	int getMacLength();

	String getProviderName();

}
