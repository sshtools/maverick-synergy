package com.sshtools.common.files.direct;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.PosixPermissions;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.UnsignedInteger64;

public final class NioFile implements AbstractFile {

	private Path path;
	private Path home;
	private final NioFileFactory fileFactory;
	private boolean sandbox;

	NioFile(Path path, NioFileFactory fileFactory, Path home, boolean sandbox) throws IOException, PermissionDeniedException {
		if(sandbox) {
			if((Files.exists(path) && !path.toRealPath().startsWith(home.toRealPath())) || 
				(!Files.exists(path) && !path.startsWith(home.toRealPath()))) {
				throw new PermissionDeniedException("You cannot access paths other than your home directory");
			}
		}
		
		this.home = home;
		this.fileFactory = fileFactory;
		this.path = path;
		this.sandbox = sandbox;
		if(Files.exists(this.path)) {
			getAttributes();
		}
	}

	NioFile(String path, NioFileFactory fileFactory, Path home, boolean sandbox) throws IOException, PermissionDeniedException {
		this(home.resolve(path), fileFactory, home, sandbox);
	}

	@Override
	public void linkTo(String target) throws IOException, PermissionDeniedException {
		Files.createLink(path, target.startsWith("/") ? fileFactory.getFile(target).path : home.resolve(target));
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		Files.createSymbolicLink(path, target.startsWith("/") ? fileFactory.getFile(target).path : home.resolve(target));
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		return Files.readSymbolicLink(path).toString();
	}

	@Override
	public String getName() {
		return path.getFileName().toString();
	}

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		return Files.newInputStream(path);
	}

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		return Files.exists(path);
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		try(var stream = Files.newDirectoryStream(path)) {
			var l =  new ArrayList<AbstractFile>();
			for(var p : stream) {
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
		return Files.newOutputStream(path);
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
		}
		catch(IOException ioe) {
		}
		return false;
	}

	@Override
	public boolean isReadable() throws IOException, PermissionDeniedException {
		return (!Files.exists(path) && (path.getParent() == null || Files.isReadable(path.getParent()))) || Files.isReadable(path);
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {
		if(src instanceof NioFile) {
			Files.copy(((NioFile)src).path, path);
		}
		else {
			AbstractFile.super.copyFrom(src);
		}
		
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {
		if(target instanceof NioFile) {
			Files.move(path, ((NioFile)target).path);
		}
		else {
			AbstractFile.super.moveTo(target);
		}
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		if(recursive)
			return IOUtils.silentRecursiveDelete(path);
		else {
			try {
				Files.delete(path);
				return true;
			}
			catch(IOException ioe) {
			}
			return false;
		}
	}

	@Override
	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		if(!Files.exists(path))
			throw new FileNotFoundException();
		
		
		Path file = path;

		var attr = Files.readAttributes(file, BasicFileAttributes.class);
		var attrs = new SftpFileAttributes(getFileType(attr), "UTF-8");
		
		try {
		
			attrs.setTimes(new UnsignedInteger64(attr.lastAccessTime().toMillis() / 1000), 
					new UnsignedInteger64(attr.lastModifiedTime().toMillis() / 1000));
			
			attrs.setSize(new UnsignedInteger64(attr.size()));

			try {
				var posix =  Files.readAttributes(file, PosixFileAttributes.class);
				
				attrs.setGroup(posix.group().getName());
				attrs.setUsername(posix.owner().getName());
				
				attrs.setPermissions(PosixPermissionsBuilder.create().
						withPermissions(posix.permissions()).build());
				
				// We return now as we have enough information
				return attrs;
				
			} catch (UnsupportedOperationException | IOException e) {
			}

			
			try {
				var dos = Files.readAttributes(file,
							DosFileAttributes.class);
			
				var bldr = PosixPermissionsBuilder.create();
				bldr.withAllRead();
				if(!dos.isReadOnly()) {
					bldr.withAllWrite();
				}
				var filename = path.getFileName().toString();
				if(filename.endsWith(".exe") || filename.endsWith(".com") || filename.endsWith(".cmd")) {
					bldr.withAllExecute();
				}
				attrs.setPermissions(bldr.build());
		
			} catch(UnsupportedOperationException | IOException e) {
			}
			
			
		} catch (UnsupportedOperationException e) {
		}
		
		return attrs;
	}

	@Override
	public void refresh() {
	}

	@Override
	public long lastModified() throws IOException, PermissionDeniedException {
		return Files.getLastModifiedTime(path).toMillis();
	}

	@Override
	public long length() throws IOException, PermissionDeniedException {
		return Files.size(path);
	}

	@Override
	public boolean isWritable() throws IOException, PermissionDeniedException {
		return ( Files.exists(path) && Files.isWritable(path) ) || (!Files.exists(path) && (path.getParent() == null || (Files.isWritable(path.getParent()))));
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		try {
			Files.createFile(path);
			return true;
		}
		catch(IOException ioe) {
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
		if(attrs.hasModifiedTime()) {
			Files.setLastModifiedTime(path, FileTime.fromMillis(attrs.getModifiedTime().longValue() * 1000));
		}
		var newPerms = attrs.getPosixPermissions();
		if(!newPerms.equals(PosixPermissions.EMPTY)) {
			Set<PosixFilePermission> current = null;
			try {
				current = Files.getPosixFilePermissions(path);
			}
			catch(UnsupportedOperationException uoe) {
			}
			if(current != null) {
				if(!Objects.equals(current, newPerms.asPermissions()))
					Files.setPosixFilePermissions(path, newPerms.asPermissions());
			}
		}
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return path.toRealPath().toString();
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
		SeekableByteChannel channel = null; 
		if(writeAccess)
			channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		else
			channel = Files.newByteChannel(path, StandardOpenOption.READ);
		return channel;
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		return Files.newOutputStream(path, StandardOpenOption.APPEND);
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

	private int getFileType(BasicFileAttributes attr) {
		if(attr.isDirectory())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		if(attr.isRegularFile())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		if(attr.isSymbolicLink())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK;
		if(attr.isOther())
			return SftpFileAttributes.SSH_FILEXFER_TYPE_SPECIAL;
		
		return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
	}
}
