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
	private final SftpFileSystem fs;
	private volatile Iterator<Path> iterator;
	private volatile boolean open = true;
	private final Path path;

	SftpDirectoryStream(SftpPath sftpPath, DirectoryStream.Filter<? super Path> filter) throws IOException {
		this.fs = sftpPath.getFileSystem();
		this.path = sftpPath.normalize();
		this.filter = filter;
		if(Files.exists(path)) {
			if(!Files.isDirectory(path)) {
				throw new NotDirectoryException(sftpPath.toString());
			}
		}
		else
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
			iterator = fs.getFileSystemProvider().iterator(path, filter); 
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
