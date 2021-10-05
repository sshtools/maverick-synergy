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
package com.sshtools.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class BinaryLogger {

	RandomAccessFile file;
	FileChannel channel;
	
	public BinaryLogger(String name) throws FileNotFoundException {
		file = new RandomAccessFile(new File(name), "rw");
		channel = file.getChannel();
	}
	
	public void log(byte[] buf) throws IOException {
		log(buf, 0, buf.length);
	}
	
	public void log(byte[] buf, int off, int len) throws IOException {
		channel.write(ByteBuffer.wrap(buf, off, len));
	}
	
	public void log(ByteBuffer buf) throws IOException {
		channel.write(buf);
	}
	
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
		}
		try {
			file.close();
		} catch (IOException e) {
		}
	}
}
