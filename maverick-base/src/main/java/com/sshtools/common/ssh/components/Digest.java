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
package com.sshtools.common.ssh.components;

import java.math.BigInteger;

/**
 * A general interface for a digest with utility methods to add java types
 * into the digest. 
 * 
 * @author Lee David Painter
 *
 */
public interface Digest {

	/**
	 * Update the digest with a BigInteger value. This puts both the integer
	 * length of the BigInteger data and the binary data.
	 * @param bi
	 */
	public abstract void putBigInteger(BigInteger bi);

	/**
	 * Put a single byte into the digest.
	 * @param b
	 */
	public abstract void putByte(byte b);

	/**
	 * Put a byte array into the digest.
	 * @param data
	 */
	public abstract void putBytes(byte[] data);

	/**
	 * Put a byte array into the digest
	 * @param data
	 * @param offset
	 * @param len
	 */
	public abstract void putBytes(byte[] data, int offset, int len);

	/**
	 * Put an integer into the digest.
	 * @param i
	 */
	public abstract void putInt(int i);

	/**
	 * Put a String into the digest.
	 * @param str
	 */
	public abstract void putString(String str);

	/**
	 * Reset the underlying digest.
	 */
	public abstract void reset();

	/**
	 * Do the final processing and return the hash.
	 * @return the hash
	 */
	public abstract byte[] doFinal();
	
}

