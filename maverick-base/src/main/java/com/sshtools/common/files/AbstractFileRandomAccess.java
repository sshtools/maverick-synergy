package com.sshtools.common.files;

/*-
 * #%L
 * Base API
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
