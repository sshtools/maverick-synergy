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
