package com.sshtools.common.files.direct;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.files.FileVolume;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.PosixPermissions;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.UnsignedInteger64;

public final class NioFile implements AbstractFile {

	public static final int SSH_FXE_STATVFS_ST_RDONLY = 0x1; /* read-only */
	public static final int SSH_FXE_STATVFS_ST_NOSUID = 0x2; /* no setuid */
	
	private Path path;
	private Path home;
	private final NioFileFactory fileFactory;
	private boolean sandbox;

	NioFile(Path path, NioFileFactory fileFactory, Path home, boolean sandbox)
			throws IOException, PermissionDeniedException {
		if (sandbox) {
			if ((Files.exists(path) && !path.toRealPath().startsWith(home.toRealPath()))
					|| (!Files.exists(path) && !path.startsWith(home.toRealPath()))) {
				throw new PermissionDeniedException("You cannot access paths other than your home directory");
			}
		}

		this.home = home;
		this.fileFactory = fileFactory;
		this.path = path;
		this.sandbox = sandbox;
		if (Files.exists(this.path)) {
			getAttributes();
		}
	}

	NioFile(String path, NioFileFactory fileFactory, Path home, boolean sandbox)
			throws IOException, PermissionDeniedException {
		this(home.resolve(path), fileFactory, home, sandbox);
	}

	@Override
	public boolean existsNoFollowLinks() throws IOException, PermissionDeniedException {
		return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
	}

	@Override
	public void linkTo(String target) throws IOException, PermissionDeniedException {
		try {
			Files.createLink(target.startsWith("/") ? fileFactory.getFile(target).path : home.resolve(target), path);
		} catch (IOException nfe) {
			throw translateException(nfe);
		}
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		try {
			Files.createSymbolicLink(target.startsWith("/") ? fileFactory.getFile(target).path : home.resolve(target),
					path);
		} catch (IOException nfe) {
			throw translateException(nfe);
		}
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		try {
			return Files.readSymbolicLink(path).toString();
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public String getName() {
		return path.getFileName().toString();
	}

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		try {
			return Files.newInputStream(path);
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		return Files.exists(path);
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		try (var stream = Files.newDirectoryStream(path)) {
			var l = new ArrayList<AbstractFile>();
			for (var p : stream) {
				l.add(new NioFile(p, fileFactory, home, sandbox));
			}
			return Collections.unmodifiableList(l);
		}
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return path.toAbsolutePath().toString();
	}

	@Override
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		return path.getParent() == null ? null : new NioFile(path.getParent(), fileFactory, home, sandbox);
	}

	@Override
	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return Files.isDirectory(path);
	}

	@Override
	public boolean isFile() throws IOException, PermissionDeniedException {
		return Files.isRegularFile(path);
	}

	@Override
	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		try {
			return Files.newOutputStream(path);
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public boolean isHidden() throws IOException, PermissionDeniedException {
		return Files.isHidden(path);
	}

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		try {
			Files.createDirectories(path);
			return true;
		} catch (IOException ioe) {
		}
		return false;
	}

	@Override
	public boolean isReadable() throws IOException, PermissionDeniedException {
		return (!Files.exists(path) && (path.getParent() == null || Files.isReadable(path.getParent())))
				|| Files.isReadable(path);
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {
		try {
			if (src instanceof NioFile) {
				Files.copy(((NioFile) src).path, path);
			} else {
				AbstractFile.super.copyFrom(src);
			}
		} catch (IOException ioe) {
			throw translateException(ioe);
		}

	}

	@Override
	public void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {
		try {
			if (target instanceof NioFile) {
				Files.move(path, ((NioFile) target).path);
			} else {
				AbstractFile.super.moveTo(target);
			}
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		if (recursive)
			return IOUtils.silentRecursiveDelete(path);
		else {
			try {
				Files.delete(path);
				return true;
			} catch (IOException ioe) {
			}
			return false;
		}
	}


	@Override
	public SftpFileAttributes getAttributesNoFollowLinks() throws FileNotFoundException, IOException, PermissionDeniedException {
		if (!existsNoFollowLinks())
			throw new FileNotFoundException();
		return doGetAttributes();
	}

	@Override
	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		if (!exists())
			throw new FileNotFoundException();
		return doGetAttributes();

	}

	protected SftpFileAttributes doGetAttributes() throws FileNotFoundException, IOException {

		try {

			Path file = path;

			var attr = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			var attrs = new SftpFileAttributes(getFileType(attr), "UTF-8");

			try {

				attrs.setSize(new UnsignedInteger64(attr.size()));

				try {
					var posix = Files.readAttributes(file, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

					attrs.setGroup(posix.group().getName());
					attrs.setUsername(posix.owner().getName());

					attrs.setTimes(new UnsignedInteger64(posix.lastAccessTime().toMillis() / 1000),
							new UnsignedInteger64(posix.lastModifiedTime().toMillis() / 1000),
							new UnsignedInteger64(posix.creationTime().toMillis() / 1000));
					attrs.setPermissions(PosixPermissionsBuilder.create().withPermissions(posix.permissions()).build());

					// We return now as we have enough information
					return attrs;

				} catch (UnsupportedOperationException | IOException e) {
				}

				attrs.setTimes(new UnsignedInteger64(attr.lastAccessTime().toMillis() / 1000),
						new UnsignedInteger64(attr.lastModifiedTime().toMillis() / 1000),
						new UnsignedInteger64(attr.creationTime().toMillis() / 1000));

				try {
					var dos = Files.readAttributes(file, DosFileAttributes.class);

					var bldr = PosixPermissionsBuilder.create();
					bldr.withAllRead();
					if (!dos.isReadOnly()) {
						bldr.withAllWrite();
					}
					var filename = path.getFileName().toString();
					if (filename.endsWith(".exe") || filename.endsWith(".com") || filename.endsWith(".cmd")) {
						bldr.withAllExecute();
					}
					attrs.setPermissions(bldr.build());

				} catch (UnsupportedOperationException | IOException e) {
				}

			} catch (UnsupportedOperationException e) {
			}
			return attrs;
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public void refresh() {
	}

	@Override
	public long lastModified() throws IOException, PermissionDeniedException {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public long length() throws IOException, PermissionDeniedException {
		try {
			return Files.size(path);
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public boolean isWritable() throws IOException, PermissionDeniedException {
		return (Files.exists(path) && Files.isWritable(path))
				|| (!Files.exists(path) && (path.getParent() == null || (Files.isWritable(path.getParent()))));
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		try {
			Files.createFile(path);
			return true;
		} catch (IOException ioe) {
		}
		return false;
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		Files.deleteIfExists(path);
		Files.createFile(path);
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		try {
			var basicView = Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
			basicView.setTimes(
					attrs.hasModifiedTime() ? FileTime.fromMillis(attrs.getModifiedDateTime().getTime()) : null,
					attrs.hasAccessTime() ? FileTime.fromMillis(attrs.getAccessedDateTime().getTime()) : null,
					attrs.hasCreateTime() ? FileTime.fromMillis(attrs.getCreationDateTime().getTime()) : null);

			if(attrs.hasUID()) {
				try {
					Files.setOwner(path, path.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(attrs.getUID()));
				} catch (UnsupportedOperationException | IllegalArgumentException | IOException e) {
				}
			}
			if(attrs.hasGID()) {
				try {
					Files.setAttribute(path, "posix:group", path.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(attrs.getUID()));
				} catch (UnsupportedOperationException | IllegalArgumentException | IOException e) {
				}
			}
			
			var newPerms = attrs.getPosixPermissions();
			if (!newPerms.equals(PosixPermissions.EMPTY)) {
				Set<PosixFilePermission> current = null;
				try {
					current = Files.getPosixFilePermissions(path);
				} catch (UnsupportedOperationException uoe) {
				}
				if (current != null) {
					if (!Objects.equals(current, newPerms.asPermissions()))
						Files.setPosixFilePermissions(path, newPerms.asPermissions());
				}
			}
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		try {
			return path.toRealPath().toString();
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public boolean supportsRandomAccess() {
		return true;
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException {
		var channel = createChannel(writeAccess);
		return new AbstractFileRandomAccess() {

			@Override
			public void write(byte[] buf, int off, int len) throws IOException {
				channel.write(ByteBuffer.wrap(buf, off, len));
			}

			@Override
			public void setLength(long length) throws IOException {
				channel.truncate(length);
			}

			@Override
			public void seek(long position) throws IOException {
				channel.position(position);
			}

			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				return channel.read(ByteBuffer.wrap(buf, off, len));
			}

			@Override
			public long getFilePointer() throws IOException {
				return channel.position();
			}

			@Override
			public void close() throws IOException {
				channel.close();
			}
		};
	}

	private SeekableByteChannel createChannel(boolean writeAccess) throws IOException {
		try {
			SeekableByteChannel channel = null;
			if (writeAccess)
				channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			else
				channel = Files.newByteChannel(path, StandardOpenOption.READ);
			return channel;
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		try {
			return Files.newOutputStream(path, StandardOpenOption.APPEND);
		} catch (IOException ioe) {
			throw translateException(ioe);
		}
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		return new NioFile(path.resolve(child), fileFactory, home, sandbox);
	}

	@Override
	public AbstractFileFactory<NioFile> getFileFactory() {
		return fileFactory;
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NioFile other = (NioFile) obj;
		return Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return path.toString();
	}

	@Override
	public FileVolume getVolume() throws IOException {
		var nativeStore = path.getFileSystem().provider().getFileStore(path);
		return new FileVolume() {
			
			@Override
			public long userFreeInodes() {
				return freeInodes();
			}
			
			@Override
			public long userFreeBlocks() {
				try {
					return nativeStore.getUsableSpace() / blockSize();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			
			@Override
			public long underlyingBlockSize() {
				return blockSize();
			}
			
			@Override
			public long totalInodes() {
				return 0;
			}
			
			@Override
			public long maxFilenameLength() {
				/* TODO check other os */
				return 255;
			}
			
			@Override
			public long id() {
				return Integer.toUnsignedLong(nativeStore.hashCode());
			}
			
			@Override
			public long freeInodes() {
				return 0;
			}
			
			@Override
			public long freeBlocks() {
				try {
					return nativeStore.getUnallocatedSpace() / blockSize();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			
			@Override
			public long flags() {
				return nativeStore.isReadOnly() ? SSH_FXE_STATVFS_ST_RDONLY : 0;
			}
			
			@Override
			public long blocks() {
				try {
					return nativeStore.getTotalSpace() / blockSize();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			
			@Override
			public long blockSize() {
				try {
					return nativeStore.getBlockSize();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
	}

	private int getFileType(BasicFileAttributes attr) {
		if (attr.isSymbolicLink())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK;
		if (attr.isOther())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SPECIAL;
		if (attr.isDirectory())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		if (attr.isRegularFile())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;

		return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
	}

	private IOException translateException(IOException nfe) throws IOException {
		if (nfe instanceof NoSuchFileException) {
			return new FileNotFoundException(((NoSuchFileException) nfe).getFile());
		} else
			return nfe;
	}
}
