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

import java.math.BigInteger;

/**
 * A general interface for a digest with utility methods to add java types
 * into the digest. 
 * 
 * @author Lee David Painter
 *
 */
public interface Digest extends Component {
	
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

