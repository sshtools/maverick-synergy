package com.sshtools.client;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import java.io.IOException;
import java.io.InputStream;

import com.sshtools.common.files.AbstractFileRandomAccess;

public class ChunkInputStream extends InputStream {

	AbstractFileRandomAccess file;
	long length;
	
	public ChunkInputStream(AbstractFileRandomAccess file, long length) {
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
