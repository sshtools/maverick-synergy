
package com.sshtools.common.files;

import java.io.IOException;

public interface AbstractFileRandomAccess {
	public int read(byte[] buf, int off, int len) throws IOException;
	public void write(byte[] buf, int off, int len) throws IOException;
	public void setLength(long length) throws IOException;
	public void seek(long position) throws IOException;
	public void close() throws IOException;
	public long getFilePointer() throws IOException;
}
