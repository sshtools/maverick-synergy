/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
			length--;
			return file.read();
		}
		return -1;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {	
		if(length > 0) {
			int max = (int) Math.min(len, length);
			length-=max;
			
			return file.read(b, off, max);
		}
		return -1;
	}

}
