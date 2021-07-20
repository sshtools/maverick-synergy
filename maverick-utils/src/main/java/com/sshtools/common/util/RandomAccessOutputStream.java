
package com.sshtools.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RandomAccessOutputStream extends OutputStream {

	RandomAccessFile f;
	
	public RandomAccessOutputStream(RandomAccessFile f) {
		this.f = f;
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		f.write(b, off, len);
	}
	
	@Override
	public void write(int b) throws IOException {
		f.write(b);
	}

	@Override
	public void close() throws IOException {
		f.close();
	}
}
