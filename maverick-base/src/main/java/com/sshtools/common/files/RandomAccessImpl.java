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

package com.sshtools.common.files;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessImpl implements AbstractFileRandomAccess {
	
	protected RandomAccessFile raf;
	protected File f;
	
	public RandomAccessImpl(File f, boolean writeAccess) throws IOException {
		this.f = f;
		String mode = "r" + (writeAccess ? "w" : "");
		raf = new RandomAccessFile(f, mode);
	}
	public void write(int b) throws IOException {
		raf.write(b);
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		raf.write(buf, off, len);
	}

	public void close() throws IOException {
		raf.close();
	}
	
	public void seek(long position) throws IOException {
		raf.seek(position);
	}
	
	public int read(byte[] buf, int off, int len) throws IOException {
		return raf.read(buf, off, len);
	}
	
	public void setLength(long length) throws IOException {
		raf.setLength(length);
	}
	public long getFilePointer() throws IOException {
		return raf.getFilePointer();
	}
}