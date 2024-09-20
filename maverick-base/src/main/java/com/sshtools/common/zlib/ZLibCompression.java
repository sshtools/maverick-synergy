package com.sshtools.common.zlib;

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
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.sshtools.common.ssh.compression.SshCompressionFactory;
import com.sshtools.common.ssh.compression.SshCompression;

public class ZLibCompression implements SshCompression {
	
	private static final String ALGORITHM = "zlib";

	public static class ZLibCompressionFactory implements SshCompressionFactory<ZLibCompression> {

		@Override
		public ZLibCompression create() throws NoSuchAlgorithmException, IOException {
			return new ZLibCompression();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	private Inflater inflater;
	private Deflater deflater;

	public ZLibCompression() {
	}

	public String getAlgorithm() {
		return ALGORITHM;
	}

	static private final int BUF_SIZE = 65535;

	private ByteBuffer compressOut = ByteBuffer.allocate(BUF_SIZE);
	private ByteBuffer uncompressOut = ByteBuffer.allocate(BUF_SIZE);

	public void init(int type, int level) {
		if (type == SshCompression.DEFLATER) {
			deflater = new Deflater(level);
		} else if (type == SshCompression.INFLATER) {
			inflater = new Inflater();
		}
	}

	public byte[] compress(byte[] buf, int start, int len) throws IOException {
		compressOut.clear();
		deflater.setInput(buf, start, len);

		while (deflater.deflate(compressOut, Deflater.SYNC_FLUSH) > 0)
			;

		var b = new byte[compressOut.position()];
		compressOut.flip();
		compressOut.get(b);
		return b;
	}

	public byte[] uncompress(byte[] buffer, int start, int length) throws IOException {
		uncompressOut.clear();
		inflater.setInput(buffer, start, length);
		try {
			while (inflater.inflate(uncompressOut) > 0)
				;
		} catch (DataFormatException e) {
			throw new IOException("Failed to uncompress.", e);
		}
		var b = new byte[uncompressOut.position()];
		uncompressOut.flip();
		uncompressOut.get(b, 0, b.length);
		return b;
	}

}
