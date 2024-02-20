package com.sshtools.client.sftp;

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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * {@link FileVisitor} for use with {@link SftpFile}.
 */
public interface SftpFileVisitor extends FileVisitor<SftpFile> {
	@Override
	default FileVisitResult preVisitDirectory(SftpFile dir, BasicFileAttributes attrs) throws IOException {
		Objects.requireNonNull(dir);
		Objects.requireNonNull(attrs);
		return FileVisitResult.CONTINUE;
	}

	@Override
	default FileVisitResult visitFileFailed(SftpFile file, IOException exc) throws IOException {
		Objects.requireNonNull(file);
		throw exc;
	}

	@Override
	default FileVisitResult postVisitDirectory(SftpFile dir, IOException exc) throws IOException {
		Objects.requireNonNull(dir);
		if (exc != null)
			throw exc;
		return FileVisitResult.CONTINUE;
	}
}
