package com.sshtools.common.files;

import java.io.Closeable;
import java.io.IOException;

public interface AbstractFileRandomAccess extends Closeable {
	public int read(byte[] buf, int off, int len) throws IOException;
	public void write(byte[] buf, int off, int len) throws IOException;
	public void setLength(long length) throws IOException;
	public void seek(long position) throws IOException;
	public long getFilePointer() throws IOException;
	public int read() throws IOException;
}
