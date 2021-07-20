
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

