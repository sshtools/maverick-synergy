package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2023 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
