package com.sshtools.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class ChunkInputStream extends InputStream {

	RandomAccessFile file;
	long length;
	
	public ChunkInputStream(RandomAccessFile file, long length) {
		this.file = file;
		this.length = length;
	}
	
	@Override
	public int available() throws IOException {
		return length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)length;
	}

	@Override
	public int read() throws IOException {
		if(length > 0) {
			int r = file.read();
			length--;
			return r;
		}
		return -1;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {	
		if(length > 0) {
			int max = (int) Math.min(len, length);
			int r = file.read(b, off, max);
			length -= r;
			return r;
		}
		return -1;
	}

}
