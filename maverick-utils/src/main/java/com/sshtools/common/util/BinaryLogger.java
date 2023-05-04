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
