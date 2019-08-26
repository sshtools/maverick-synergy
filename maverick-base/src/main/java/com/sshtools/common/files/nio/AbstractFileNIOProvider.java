package com.sshtools.common.files.nio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class AbstractFileNIOProvider extends FileSystemProvider {

	static Map<String,FileSystem> existingFilesystems = new HashMap<>();
	
	public AbstractFileNIOProvider() {}

	@Override
	public String getScheme() {
		return AbstractFileURI.URI_SCHEME;
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		
		
		if(Objects.isNull(uri.getAuthority())) {
			throw new IOException("Missing connection id in URI authority");
		}
		
		SshConnection con = (SshConnection) env.get("connection");
		if(Objects.isNull(con)) {
			throw new IOException("Missing connection object in file system environment");
		}
		
		if(!con.getUUID().equals(uri.getAuthority())) {
			throw new IOException("Incorrect connection id in URI authority");
		}
		
		existingFilesystems.put(con.getUUID(), new AbstractFileNIOFileSystem(con, uri, this));
		return existingFilesystems.get(con.getUUID());
	}

	static final AbstractFilePath toAbstractFilePath(Path path) {
		if (path == null)
			throw new NullPointerException();
		if (!(path instanceof AbstractFilePath))
			throw new ProviderMismatchException();
		return (AbstractFilePath) path;
	}
	
	@Override
	public FileSystem getFileSystem(URI uri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path getPath(URI uri) {
		AbstractFileURI parsedUri = AbstractFileURI.parse(uri);
		return existingFilesystems.get(parsedUri.getConnectionId()).getPath(parsedUri.getPath());
	}

	@Override
	public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
		List<OpenOption> optlist = Arrays.asList(options);
		if (optlist.contains(StandardOpenOption.WRITE))
			throw new IllegalArgumentException(String.format("%s is not supported by this method.", StandardOpenOption.WRITE));
		checkAccess(path, AccessMode.READ);
		return toAbstractFilePath(path).getAbstractFile().getInputStream();
	}

	@Override
	public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
		List<OpenOption> optlist = Arrays.asList(options);
		if (optlist.contains(StandardOpenOption.READ))
			throw new IllegalArgumentException(String.format("%s is not supported by this method.", StandardOpenOption.READ));
		AbstractFile fo = toAbstractFilePath(path).getAbstractFile();
		if (optlist.contains(StandardOpenOption.CREATE_NEW) && fo.exists())
			throw new IOException(
					String.format("%s already exists, and the option %s was specified.", fo, StandardOpenOption.CREATE_NEW));
		try {
			fo.createNewFile();
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
		checkAccess(path, AccessMode.WRITE);
		return fo.getOutputStream(optlist.contains(StandardOpenOption.APPEND));
	}
	
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		return new AbstractFileSeekableByteChannel(toAbstractFilePath(path).getAbstractFile());
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		return new AbstractFileDirectoryStream(toAbstractFilePath(dir), filter);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		
		try {
			toAbstractFilePath(dir).getAbstractFile().createFolder();
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
		
	}

	@Override
	public void delete(Path path) throws IOException {
		
		try {
			toAbstractFilePath(path).getAbstractFile().delete(false);
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		
		try {
			toAbstractFilePath(target).getAbstractFile().copyFrom(toAbstractFilePath(source).getAbstractFile());
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
		
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		
		try {
			toAbstractFilePath(source).getAbstractFile().moveTo(toAbstractFilePath(target).getAbstractFile());
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		try {
			return toAbstractFilePath(path).getAbstractFile().getCanonicalPath().equals(toAbstractFilePath(path2).getAbstractFile().getCanonicalPath());
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return toAbstractFilePath(path).getAbstractFile().isHidden();
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		
		AbstractFilePath p = toAbstractFilePath(path);
		AbstractFile file = p.getAbstractFile();
		if(file==null || !file.exists()) {
			throw new FileNotFoundException();
		}
		for (AccessMode m : modes) {
			switch (m) {
//			case EXECUTE:
//				if (!file.isExecutable())
//					throw new AccessDeniedException(String.format("No %s access to %s", m, path));
//				break;
			case READ:
				if (!file.isReadable())
					throw new AccessDeniedException(String.format("No %s access to %s", m, path));
				break;
			case WRITE:
				if (!file.isWritable())
					throw new AccessDeniedException(String.format("No %s access to %s", m, path));
				break;
			default:
				break;
			}
		}
		
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		return AbstractFileAttributeView.get(toAbstractFilePath(path), type);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		if (type == BasicFileAttributes.class || type == AbstractFileBasicAttributes.class)
			return (A) toAbstractFilePath(path).getAttributes();
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		return toAbstractFilePath(path).readAttributes(attributes, options);
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		
	}

}
