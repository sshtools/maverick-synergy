package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.files.direct.NioFile;
import com.sshtools.common.files.direct.NioFileFactory;

/**
 * Deprecated. Use {@link NioFileFactory} and {@link NioFile}.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
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