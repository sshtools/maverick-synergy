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
package com.sshtools.common.files.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.IOUtils;

public class AbstractFileSeekableByteChannel implements SeekableByteChannel {

	AbstractFile file;
	File tmpfile;
	SeekableByteChannel tmp;
	
	AbstractFileSeekableByteChannel(AbstractFile file) throws IOException {
		this.file = file;
		tmp = Files.newByteChannel((tmpfile = File.createTempFile("abfs", "tmp")).toPath());
	}
	
	@Override
	public boolean isOpen() {
		return tmp.isOpen();
	}

	@Override
	public void close() throws IOException {
		tmp.close();
		try(OutputStream out = file.getOutputStream()) {
			try(InputStream in = new FileInputStream(tmpfile)) {
				IOUtils.copy(in, out);
			}
		} catch (PermissionDeniedException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return tmp.read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return tmp.write(src);
	}

	@Override
	public long position() throws IOException {
		return tmp.position();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		return tmp.position(newPosition);
	}

	@Override
	public long size() throws IOException {
		return tmp.size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		return tmp.truncate(size);
	}

}
