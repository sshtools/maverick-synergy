/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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

