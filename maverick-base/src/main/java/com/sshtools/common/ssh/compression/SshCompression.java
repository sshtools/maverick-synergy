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
package com.sshtools.common.ssh.compression;

import java.io.IOException;

import com.sshtools.common.ssh.components.SshComponent;

/**
 *
 * <p>
 * Compression interface which can be implemented to provide the SSH Transport
 * Protocol with compression.
 * </p>
 * 
 * @author Lee David Painter
 */
public interface SshCompression extends SshComponent {

	/**
	 * Inflation mode
	 */
	static public final int INFLATER = 0;

	/**
	 * Deflation mode
	 */
	static public final int DEFLATER = 1;

	/**
	 * Initialize the compression.
	 * 
	 * @param type  the mode of the compression, should be either INFLATER or
	 *              DEFLATER
	 * @param level the level of compression
	 */
	public void init(int type, int level);

	/**
	 * Compress a block of data.
	 * 
	 * @param data  the data to compress
	 * @param start the offset of the data to compress
	 * @param len   the length of the data
	 * @return the compressed data with any uncompressed data at the start remaining
	 *         intact.
	 * @throws IOException
	 */
	public byte[] compress(byte[] data, int start, int len) throws IOException;

	/**
	 * Uncompress a block of data.
	 * 
	 * @param data  the data to uncompress
	 * @param start the offset of the data to uncompress
	 * @param len   the length of the data
	 * @return the uncompressed data with any data not compressed at the start
	 *         remaining intact.
	 * @throws IOException
	 */
	public byte[] uncompress(byte[] data, int start, int len) throws IOException;

	/**
	 * Get the algorithm name for this compression implementation.
	 * 
	 * @return the algorithm name.
	 */
	public String getAlgorithm();

}
