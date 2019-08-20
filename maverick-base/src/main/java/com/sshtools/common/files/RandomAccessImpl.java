/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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