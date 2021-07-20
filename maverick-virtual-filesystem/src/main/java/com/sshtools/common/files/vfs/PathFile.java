
package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public class PathFile implements AbstractFile {
	private Path path;
	private PathFileFactory factory;

	public PathFile(Path path, PathFileFactory factory) {
		this.path = path;
		this.factory = factory;
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {
		Files.copy(src.getInputStream(), path);
	}

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		Files.createDirectory(path);
		return isDirectory();
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		Files.createFile(path);
		return isFile();
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		// TODO recursive
		return Files.deleteIfExists(path);
	}

	@Override
	public boolean exists() throws IOException {
		return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return path.toString();
	}

	public SftpFileAttributes getAttributes() throws IOException {
		int type = getFileType();
		SftpFileAttributes attrs = new SftpFileAttributes(type, "UTF-8");
		long len = 0;
		long mod = 0;
		if (type != SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN) {
			mod = lastModified();
			len = length();
		}
		// Extended attributes
		try {
			attrs.setGID(String.valueOf(Files.getAttribute(path, "unix:gid", LinkOption.NOFOLLOW_LINKS)));
		} catch (UnsupportedOperationException | IllegalArgumentException uoe) {
		}
		try {
			attrs.setGroup(String.valueOf(Files.getAttribute(path, "unix:group", LinkOption.NOFOLLOW_LINKS)));
		} catch (UnsupportedOperationException | IllegalArgumentException uoe) {
		}
		try {
			attrs.setUID(String.valueOf(Files.getAttribute(path, "unix:uid", LinkOption.NOFOLLOW_LINKS)));
		} catch (UnsupportedOperationException | IllegalArgumentException uoe) {
		}
		try {
			attrs.setUsername(String.valueOf(Files.getAttribute(path, "unix:owner", LinkOption.NOFOLLOW_LINKS)));
		} catch (UnsupportedOperationException | IllegalArgumentException uoe) {
		}
		// Permissions
		long perm = 0;
		try {
			Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
			for (PosixFilePermission pfp : perms) {
				switch (pfp) {
				case OWNER_READ:
					perm = perm | SftpFileAttributes.S_IRUSR;
					break;
				case OWNER_WRITE:
					perm = perm | SftpFileAttributes.S_IWUSR;
					break;
				case OWNER_EXECUTE:
					perm = perm | SftpFileAttributes.S_IXUSR;
					break;
				case GROUP_READ:
					perm = perm | SftpFileAttributes.S_IRGRP;
					break;
				case GROUP_WRITE:
					perm = perm | SftpFileAttributes.S_IWGRP;
					break;
				case GROUP_EXECUTE:
					perm = perm | SftpFileAttributes.S_IXGRP;
					break;
				case OTHERS_READ:
					perm = perm | SftpFileAttributes.S_IROTH;
					break;
				case OTHERS_WRITE:
					perm = perm | SftpFileAttributes.S_IWOTH;
					break;
				case OTHERS_EXECUTE:
					perm = perm | SftpFileAttributes.S_IXOTH;
					break;
				}
			}
		} catch (UnsupportedOperationException uoe) {
			if (isReadable())
				perm = perm | SftpFileAttributes.S_IRUSR | SftpFileAttributes.S_IRGRP | SftpFileAttributes.S_IROTH;
			if (isWritable())
				perm = perm | SftpFileAttributes.S_IWUSR | SftpFileAttributes.S_IWGRP | SftpFileAttributes.S_IWOTH;
			if (Files.isExecutable(path))
				perm = perm | SftpFileAttributes.S_IXUSR | SftpFileAttributes.S_IXGRP | SftpFileAttributes.S_IWOTH;
		}
		switch (type) {
		case SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY:
			perm = perm | SftpFileAttributes.S_IFDIR;
			break;
		case SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK:
			perm = perm | SftpFileAttributes.S_IFLNK;
			break;
		case SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR:
			perm = perm | SftpFileAttributes.S_IFREG;
			break;
		}
		// public static final int S_IFSOCK = 0xC000;
		// public static final int S_IFBLK = 0x6000;
		// public static final int S_IFCHR = 0x2000;
		// public static final int S_IFIFO = 0x1000;
		attrs.setPermissions(new UnsignedInteger32(perm));
		attrs.setSize(new UnsignedInteger64(len));
		attrs.setTimes(new UnsignedInteger64(mod / 1000), new UnsignedInteger64(mod / 1000));
		return attrs;
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		if(Files.exists(path))
			return path.toRealPath().toString();
		else
			return path.toString();
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		List<AbstractFile> l = new ArrayList<AbstractFile>();
		for (Path p : Files.newDirectoryStream(path)) {
			l.add(new PathFile(p, factory));
		}
		return l;
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return factory;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Files.newInputStream(path);
	}

	@Override
	public String getName() {
		return path.getFileName().toString();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(path);
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException {
		if (append)
			return Files.newOutputStream(path, StandardOpenOption.APPEND);
		else
			return Files.newOutputStream(path);
	}

	@Override
	public boolean isDirectory() throws IOException {
		return Files.isDirectory(path);
	}

	@Override
	public boolean isFile() throws IOException {
		return Files.isRegularFile(path);
	}

	@Override
	public boolean isHidden() throws IOException {
		return Files.isHidden(path);
	}

	@Override
	public boolean isReadable() throws IOException {
		return Files.isReadable(path);
	}

	@Override
	public boolean isWritable() throws IOException {
		return (!Files.exists(path) && (path.getParent() == null || Files.isWritable(path.getParent()))) || Files.isWritable(path);
	}

	@Override
	public long lastModified() throws IOException {
		return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).to(TimeUnit.MILLISECONDS);
	}

	@Override
	public long length() throws IOException {
		return Files.exists(path) ? Files.size(path) : 0;
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {
		if (target instanceof PathFile)
			Files.move(path, ((PathFile) target).path);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException {
		return new PathRandomAccessImpl(path, writeAccess);
	}

	@Override
	public void refresh() {
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		return new PathFile(this.path.resolve(child), factory);
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		if (attrs.hasModifiedTime()) {
			Files.setLastModifiedTime(path, FileTime.from(attrs.getModifiedTime().longValue(), TimeUnit.SECONDS));
		}
		if(attrs.hasGID()) {
			try {
				Files.setAttribute(path, "unix:gid", attrs.getGID(), LinkOption.NOFOLLOW_LINKS);
			} catch (UnsupportedOperationException | IllegalArgumentException uoe) {
			}
		}
		if(attrs.hasUID()) {
			try {
				Files.setAttribute(path, "unix:uid", attrs.getUID(), LinkOption.NOFOLLOW_LINKS);
			} catch (UnsupportedOperationException | IllegalArgumentException uoe) {
			}
		}
	}

	@Override
	public boolean supportsRandomAccess() {
		return true;
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		delete(false);
		createNewFile();
	}

	private int getFileType() throws IOException {
		if (isDirectory()) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		} else if (Files.isSymbolicLink(path)) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK;
		} else if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		} else {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
		}
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		Files.createSymbolicLink(path, factory.getFile(target).path);
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		return Files.readSymbolicLink(path).toString();
	}
}
