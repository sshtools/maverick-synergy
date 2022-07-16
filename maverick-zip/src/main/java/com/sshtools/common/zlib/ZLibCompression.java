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

package com.sshtools.common.zlib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.sshtools.common.ssh.compression.SshCompression;

public class ZLibCompression implements SshCompression {

	private Inflater inflater;
	private Deflater deflater;

	public ZLibCompression() {
	}

	public String getAlgorithm() {
		return "zlib";
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
