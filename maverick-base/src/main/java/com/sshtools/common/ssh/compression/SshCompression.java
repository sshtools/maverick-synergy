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
