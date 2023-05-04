package com.sshtools.client.sftp;
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
