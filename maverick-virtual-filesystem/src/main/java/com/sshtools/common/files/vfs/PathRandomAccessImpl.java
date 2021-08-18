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

package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.sshtools.common.files.AbstractFileRandomAccess;

public class PathRandomAccessImpl implements AbstractFileRandomAccess {
	
	protected FileChannel raf;
	protected Path f;
	
	public PathRandomAccessImpl(Path f, boolean writeAccess) throws IOException {
		this.f = f;
		if(writeAccess)
			raf = FileChannel.open(f, StandardOpenOption.READ, StandardOpenOption.WRITE);
		else
			raf = FileChannel.open(f, StandardOpenOption.READ);
	}
	
	public void write(int b) throws IOException {
		raf.write(ByteBuffer.wrap(new byte[] {(byte)b}));
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		raf.write(ByteBuffer.wrap(buf, off, len));
	}

	public void close() throws IOException {
		raf.close();
	}
	
	public void seek(long position) throws IOException {
		raf.position(position);
	}
	
	public int read(byte[] buf, int off, int len) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(len);
		int r = raf.read(b);
		if(r != -1) {
			byte[] arr = b.array(); 
			System.arraycopy(arr, 0, buf, off, arr.length);
		}
		return r;
	}
	
	public void setLength(long length) throws IOException {
		raf.truncate(length);
	}
	
	public long getFilePointer() throws IOException {
		return raf.position();
	}
}