/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2023 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.niofs;

/*-
 * #%L
 * Java NIO File System ImplemnentationI
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import static com.sshtools.synergy.niofs.SftpFileSystem.toAbsolutePathString;
import static com.sshtools.synergy.niofs.SftpFileSystemProvider.translateException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SftpDirectoryStream implements DirectoryStream<Path> {
	private final DirectoryStream.Filter<? super Path> filter;
	private volatile Iterator<Path> iterator;
	private volatile boolean open = true;
	private final Path path;

	SftpDirectoryStream(SftpPath sftpPath, DirectoryStream.Filter<? super Path> filter) throws IOException {
		this.path = sftpPath.normalize();
		this.filter = filter;
		if (Files.exists(path)) {
			if (!Files.isDirectory(path)) {
				throw new NotDirectoryException(sftpPath.toString());
			}
		} else
			throw new NoSuchFileException(sftpPath.toString());
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
			var sftpPath = (SftpPath) path;
			try {
				var fs = sftpPath.getFileSystem();
				var pstr = toAbsolutePathString(path);
				var sftp = fs.getSftp();
				var it = sftp.lsIterator(pstr);

				iterator = new Iterator<>() {

					Path next = null;

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean hasNext() {
						if (!open)
							return false;
						checkNext();
						return next != null;
					}

					@Override
					public Path next() {
						if (!open)
							throw new NoSuchElementException();
						try {
							checkNext();
							if (next == null) {
								throw new NoSuchElementException();
							}
							return next;
						} finally {
							next = null;
						}
					}

					private void checkNext() {
						if (next == null) {
							while (true) {
								var hasNext = it.hasNext();
								if (hasNext) {
									var nextFile = it.next();
									/* TODO: check this will never actual happen */
									/*if (nextFile.getFilename().equals(".") || nextFile.getFilename().equals(".."))
										continue; */
									var p = path.resolve(nextFile.getFilename());
									try {
										if (filter == null || filter.accept(p)) {
											next = p;
											return;
										}
									} catch (IOException ioe) {
										throw new UncheckedIOException(ioe);
									}
								} else
									return;
							}
						}
					}

				};
			} catch (Exception e) {
				throw translateException(e);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return iterator;
	}

}
