package com.sshtools.common.files.memory;

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

import java.io.IOException;

import com.sshtools.common.files.AbstractFileRandomAccess;

/**
 * Random-access view over an {@link InMemoryFileFactory.Node}'s byte content.
 * All reads and writes synchronize on the owning factory so they are safe
 * to use concurrently with other file operations on the same filesystem.
 */
public class InMemoryFileRandomAccess implements AbstractFileRandomAccess {

	private final InMemoryFileFactory.Node node;
	private final boolean writeAccess;
	private final Object lock;
	private long position = 0;

	InMemoryFileRandomAccess(InMemoryFileFactory.Node node, boolean writeAccess, Object lock) {
		this.node = node;
		this.writeAccess = writeAccess;
		this.lock = lock;
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		synchronized (lock) {
			if (position >= node.content.length) return -1;
			int toRead = (int) Math.min(len, node.content.length - position);
			System.arraycopy(node.content, (int) position, buf, off, toRead);
			position += toRead;
			return toRead;
		}
	}

	@Override
	public int read() throws IOException {
		synchronized (lock) {
			if (position >= node.content.length) return -1;
			return node.content[(int) position++] & 0xFF;
		}
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		if (!writeAccess) throw new IOException("File not opened for writing");
		synchronized (lock) {
			long endPos = position + len;
			if (endPos > node.content.length) {
				byte[] grown = new byte[(int) endPos];
				System.arraycopy(node.content, 0, grown, 0, node.content.length);
				node.content = grown;
			}
			System.arraycopy(buf, off, node.content, (int) position, len);
			position += len;
			node.lastModified = System.currentTimeMillis();
		}
	}

	@Override
	public void setLength(long length) throws IOException {
		if (!writeAccess) throw new IOException("File not opened for writing");
		synchronized (lock) {
			byte[] resized = new byte[(int) length];
			System.arraycopy(node.content, 0, resized, 0,
					(int) Math.min(node.content.length, length));
			node.content = resized;
			node.lastModified = System.currentTimeMillis();
		}
	}

	@Override
	public void seek(long position) throws IOException {
		this.position = position;
	}

	@Override
	public long getFilePointer() throws IOException {
		return position;
	}

	@Override
	public void close() throws IOException {
		// All writes already reflected immediately; nothing to flush.
	}
}
