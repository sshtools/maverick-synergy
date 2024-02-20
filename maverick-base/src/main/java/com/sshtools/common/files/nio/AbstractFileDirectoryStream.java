package com.sshtools.common.files.nio;

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
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class AbstractFileDirectoryStream implements DirectoryStream<Path> {
	private final DirectoryStream.Filter<? super Path> filter;
	private final AbstractFileNIOFileSystem fs;
	private volatile Iterator<Path> iterator;
	private volatile boolean open = true;
	private final Path path;

	AbstractFileDirectoryStream(AbstractFilePath path, DirectoryStream.Filter<? super Path> filter) throws IOException {
		this.fs = path.getFileSystem();
		this.path = path.normalize();
		this.filter = filter;
		if (!Files.isDirectory(path))
			throw new NotDirectoryException(path.toString());
	}

	@Override
	public synchronized void close() throws IOException {
		open = false;
	}

	@Override
	public synchronized Iterator<Path> iterator() {
		if (!open)
			throw new ClosedDirectoryStreamException();
		if (iterator != null)
			throw new IllegalStateException();
		try {
			iterator = fs.iterator(path, filter);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return new Iterator<Path>() {
			@Override
			public boolean hasNext() {
				if (!open)
					return false;
				return iterator.hasNext();
			}

			@Override
			public synchronized Path next() {
				if (!open)
					throw new NoSuchElementException();
				return iterator.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
