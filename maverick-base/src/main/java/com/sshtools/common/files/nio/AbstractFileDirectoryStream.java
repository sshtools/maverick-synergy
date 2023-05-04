package com.sshtools.common.files.nio;
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
