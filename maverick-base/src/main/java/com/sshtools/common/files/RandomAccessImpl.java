
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