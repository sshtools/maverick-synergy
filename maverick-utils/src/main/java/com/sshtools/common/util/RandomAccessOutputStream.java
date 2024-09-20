package com.sshtools.common.util;

/*-
 * #%L
 * Utils
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
