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

import static com.sshtools.common.util.Utils.emptyOptionalIfBlank;
import static com.sshtools.synergy.niofs.SftpFileSystem.toAbsolutePathString;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

public class SftpFileSystemProvider extends FileSystemProvider {

	public final static String SFTP_CLIENT = "sftp-client";
	public final static String SSH_CLIENT = "ssh-client";
	public final static String USERNAME = "username";
	public final static String PASSWORD = "password";
	public final static String HOSTNAME = "hostname";
	public final static String PORT = "port";
	public final static String PATH = "path";
	public final static String SFTP_CLOSE_ON_FS_CLOSE = "sftp-close-on-fs-close";
	public final static String RELATIVE_SYMBOLIC_LINKS = "relative-symbolic-links";

	protected static final long TRANSFER_SIZE = 8192;

	static IOException translateException(Exception e) {
		if(e instanceof SftpStatusException) {
			var sse = (SftpStatusException)e;
			switch (sse.getStatus()) {
			case SftpStatusException.SSH_FX_BYTE_RANGE_LOCK_REFUSED:
				throw new IllegalArgumentException(sse.getMessage(), sse);
			case SftpStatusException.SSH_FX_BYTE_RANGE_LOCK_CONFLICT:
				var ovfle = new OverlappingFileLockException();
				ovfle.initCause(e);
				throw ovfle;
			case SftpStatusException.SSH_FX_NO_SUCH_FILE:
				var fnfe = new NoSuchFileException(e.getMessage());
				fnfe.initCause(e);
				return fnfe;
			case SftpStatusException.SSH_FX_EOF:
				var eofe = new EOFException(e.getMessage());
				eofe.initCause(e);
				return eofe;
			case SftpStatusException.SSH_FX_DIR_NOT_EMPTY:
				var dne = new DirectoryNotEmptyException(e.getMessage());
				dne.initCause(e);
				return dne;
			case SftpStatusException.SSH_FX_OP_UNSUPPORTED:
				var uoe = new UnsupportedOperationException(e.getMessage());
				uoe.initCause(e);
				throw uoe;
			case SftpStatusException.SSH_FX_FILE_ALREADY_EXISTS:
				return new FileAlreadyExistsException(e.getMessage());
			}
			return new IOException(e.getMessage(), e);
		}
		else if(e instanceof IOException) {
			return (IOException)e;
		}
		else if(e instanceof RuntimeException) {
			throw (RuntimeException)e;
		}
		else 
			return new IOException(e.getMessage(), e);
	}

	private final Map<URI, SftpFileSystem> filesystems = Collections.synchronizedMap(new HashMap<>());

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		var modeList = Arrays.asList(modes);
		if (modeList.contains(AccessMode.EXECUTE))
			throw new AccessDeniedException("Cannot execute files on this file system.");

		var sftpPath = (SftpPath) path;
		try {
			var fs = sftpPath.getFileSystem();
			var pstr = toAbsolutePathString(path);
			fs.getSftp().stat(pstr);
			/*
			 * Just assume we can read and write. SFTP itself provides no way to test if the
			 * currently authenticated user can read or write.
			 * 
			 * We could potentially try opening the file for reading and check for any
			 * error, but writing is more risky.
			 * 
			 * If we had access to a shell we could use 'test' unix shell built-in command.
			 */
		} catch(Exception e) {
			throw translateException(e);
		}

	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		var sourceSftpPath = (SftpPath) source;
		var optionsList = Set.of(options);
		try {
			var fs = sourceSftpPath.getFileSystem();
			var sourcePath = toAbsolutePathString(source);
			var targetPath = toAbsolutePathString(target);
			var replaceExisting = optionsList.contains(StandardCopyOption.REPLACE_EXISTING);
			var sftp = fs.getSftp();

			try {
				sftp.copyRemoteFile(sourcePath, targetPath, replaceExisting);
			} catch (SftpStatusException se) {
				if (se.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
					if (!replaceExisting && Files.exists(target))
						throw new FileAlreadyExistsException(targetPath);
					try (var in = sftp.getInputStream(sourcePath)) {
						try (var out = sftp.getOutputStream(targetPath)) {
							in.transferTo(out);
						}
					}
				} else
					throw se;
			}

			if (optionsList.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
				var stat = sftp.stat(sourcePath);
				var otherStat = SftpFileAttributesBuilder.create().withFileAttributes(sftp.stat(targetPath));
				otherStat.withPermissions(stat.permissions());
				otherStat.withUidOrUsername(stat.bestUsernameOr());
				otherStat.withGidOrGroup(stat.bestGroupOr());
				sftp.getSubsystemChannel().setAttributes(targetPath, otherStat.build());
			}

		} catch (Exception e) {
			throw translateException(e);
		}

	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		var sftpPath = (SftpPath) dir;
		try {
			var fs = sftpPath.getFileSystem();
			fs.getSftp().mkdir(toAbsolutePathString(dir));
		} catch (Exception e) {
			throw translateException(e);
		}
	}

	@Override
	public void createLink(Path link, Path existing) throws IOException {
		var sftpPath = (SftpPath) link;
		try {
			var fs = sftpPath.getFileSystem();
			fs.getSftp().hardlink(toAbsolutePathString(existing), toAbsolutePathString(sftpPath));
		} catch (Exception e) {
			throw translateException(e);
		}
	}

	@Override
	public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
		var sftpPath = (SftpPath) link;
		try {
			var fs = sftpPath.getFileSystem();
			var targetPath = SftpFileSystem.toAbsolutePathString(target);
			if(target.isAbsolute()) {
				fs.getSftp().symlink(targetPath, toAbsolutePathString(sftpPath));
			}
			else {
				fs.getSftp().relativeSymlink(link.toAbsolutePath().getParent().relativize(target.toAbsolutePath()).toString(), toAbsolutePathString(sftpPath));
			}
		} catch (Exception e) {
			throw translateException(e);
		}
	}

	@Override
	public void delete(Path path) throws IOException {
		var sftpPath = (SftpPath) path;
		if (path.isAbsolute() && path.toString().equals("/"))
			throw new IOException("Cannot delete root path.");
		try {
			var fs = sftpPath.getFileSystem();
			fs.getSftp().rm(toAbsolutePathString(sftpPath));
		} catch (Exception e) {
			throw translateException(e);
		}
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		return (V)SftpFileAttributeViews.get((SftpPath) path, type);
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		var sftpPath = (SftpPath) path;
		var fs = sftpPath.getFileSystem();
		return new SftpFileStore(fs.getSftp(), toAbsolutePathString(sftpPath));
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		synchronized (filesystems) {
			var fs = filesystems.get(uri);
			if (fs == null)
				throw new FileSystemNotFoundException(
						MessageFormat.format("Cannot find SFTP file system for {0}", uri));
			return fs;
		}
	}

	@Override
	public Path getPath(URI uri) {
		if ("sftp".equals(uri.getScheme())) {
			synchronized (filesystems) {
				for(var fs : filesystems.entrySet()) {
					if( isCompatibleUri(uri, fs.getKey())) {
						return fs.getValue().getPath(uri.getPath());
					}
				}
				try {
					return newFileSystem(uri, Collections.emptyMap()).getPath("/");
				} catch (IOException e) {
					throw new FileSystemNotFoundException(MessageFormat.format("File system {0} not found.", uri));
				}
			}
		}
		throw new UnsupportedOperationException();
	}

	protected boolean isCompatibleUri(URI uri, URI other) {
		var path1 = other.getPath();
		var compatiblePath = ( path1 == null && uri.getPath() == null ) || ( path1 != null && uri.getPath() != null && uri.getPath().startsWith(path1));
		var compatibleHost = Objects.equals(uri.getHost(), other.getHost());
		var compatiblePort = Objects.equals(uri.getPort(), other.getPort());
		return compatiblePath && compatibleHost && compatiblePort;
	}

	@Override
	public String getScheme() {
		return "sftp";
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return path.getFileName().toString().startsWith(".");
	}

	@Override
	public boolean isSameFile(Path path1, Path path2) throws IOException {
		if(path1 instanceof SftpPath && path2 instanceof SftpPath && Files.exists(path1) && Files.exists(path2)) {
			var full1 = ((SftpPath)path1).toAbsolutePath();
			var full2 = path2.toAbsolutePath();
			return full1.equals(full2);
		}
		else
			return false;
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		var sourceSftpPath = (SftpPath) source;
		var optionsList = Set.of(options);
		try {
			var fs = sourceSftpPath.getFileSystem();
			var sourcePath = toAbsolutePathString(source);
			var targetPath = toAbsolutePathString(target);
			var replaceExisting = optionsList.contains(StandardCopyOption.REPLACE_EXISTING);
			var sftp = fs.getSftp();

			try {
				sftp.rename(sourcePath, targetPath, replaceExisting);
			} catch (SftpStatusException se) {
				if (se.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
					if (replaceExisting && Files.exists(target))
						Files.delete(target);
					sftp.rename(sourcePath, targetPath);
				} else
					throw se;
			}
		} catch (Exception e) {
			throw translateException(e);
		}

	}

	@Override
	public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options,
			ExecutorService exec, FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		return newFileChannel(path, options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		return new SftpDirectoryStream((SftpPath) dir, filter);
	}

	@Override
	public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {

		var sftpPath = (SftpPath) path;
		try {
			var fs = sftpPath.getFileSystem();
			var pstr = toAbsolutePathString(path);

			int flags = optionsToFlags(path, options, sftpPath);

			var deleteOnClose = options.contains(StandardOpenOption.DELETE_ON_CLOSE);
			var handle = fs.getSftp().openFile(pstr, flags);

			return new SftpFileChannel(deleteOnClose, path, handle);

		} catch (Exception e) {
			throw translateException(e);
		}

	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		synchronized (filesystems) {
			var pathStr = (String) env.get(PATH);
			if(pathStr != null) {
				uri = uri.resolve(pathStr);
			}
			
			if (filesystems.containsKey(uri))
				throw new FileSystemAlreadyExistsException();
			var sftpClient = (SftpClient) env.get(SFTP_CLIENT);
			var closeOnFsClose = (Boolean) env.get(SFTP_CLOSE_ON_FS_CLOSE);

			if (sftpClient == null) {

				if (closeOnFsClose == null)
					closeOnFsClose = true;

				var sshClient = (SshClient) env.get(SSH_CLIENT);

				if (sshClient == null) {
					String hostname = uri.getHost();
					Integer port = uri.getPort();

					if (env.containsKey(HOSTNAME))
						hostname = (String) env.get(HOSTNAME);
					if (env.containsKey(PORT))
						port = (Integer) env.get(PORT);

					if (port == -1)
						port = 22;

					String username = null;
					String password = null;
					var userInfo = uri.getRawUserInfo();
					if (userInfo != null) {
						var idx = userInfo.indexOf(':');
						if (idx == -1) {
							username = uri.getUserInfo();
						} else {
							username = decodeUserInfo(userInfo.substring(0, idx));
							password = decodeUserInfo(userInfo.substring(idx + 1));
						}
					}

					if (env.containsKey(USERNAME))
						username = (String) env.get(USERNAME);
					if (env.containsKey(PASSWORD))
						password = (String) env.get(PASSWORD);

					if (hostname != null && username != null) {
						try {
							if (password == null) {
								sshClient = SshClientBuilder.create().
										withTarget(hostname, port).
										withUsername(username).
										build();
							} else {
								sshClient = SshClientBuilder.create().
										withTarget(hostname, port).
										withUsername(username).
										withPassword(password).
										build();
							}
						} catch (SshException e) {
							throw translateException(e);
						}
					}
					else
						throw new IllegalArgumentException(MessageFormat.format(
								"Must either set a value for key {0} of type {1}, or key {2} of type to the environment. "
										+ "Alternatively, the host, port, username and password can all be expressed in the URI, i.e.. sftp://user:password@hostname/path.",
								SFTP_CLIENT, SftpClient.class.getName(), SSH_CLIENT, SshClient.class.getName()));
				}

				if (!sshClient.isConnected()) {
					if (closeOnFsClose)
						sshClient.close();
					throw new IOException("SSH client is not connected.");
				}
				if (!sshClient.isAuthenticated()) {
					if (closeOnFsClose)
						sshClient.close();
					throw new IOException("SSH client is not authenticated.");
				}
				try {
					sftpClient = SftpClientBuilder.create().withClient(sshClient).build();
				} catch (SshException | PermissionDeniedException e) {
					throw translateException(e);
				}
			} else {
				if (closeOnFsClose == null)
					closeOnFsClose = true;
			}
			var vfs = new SftpFileSystem(sftpClient, this, emptyOptionalIfBlank(uriToRootPath(uri)), closeOnFsClose, uri);
			filesystems.put(uri, vfs);
			return vfs;
		}
	}

	protected String uriToRootPath(URI uri) {
		var path = uri.getPath();
		if(path.equals("///")) 
			return "/";
		else if(path.equals("//"))
			return "";
		else if(path.startsWith("//"))
			return path.substring(1);
		else
			return path;
	}

	static String decodeUserInfo(String userinfo) {
		return URI.create("sftp://" + userinfo + "@localhost").getUserInfo();
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		var sftpPath = (SftpPath) path;
		return SftpFileAttributeViews.getAttributes(sftpPath, type);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attribute, LinkOption... options) throws IOException {
		return ((SftpPath) path).readAttributes(attribute, options);
	}

	@Override
	public Path readSymbolicLink(Path link) throws IOException {
		var sftpPath = (SftpPath) link;
		try {
			var fs = sftpPath.getFileSystem();
			return fs.getPath(fs.getSftp().getSymbolicLinkTarget(toAbsolutePathString(sftpPath)));
		} catch (Exception e) {
			throw translateException(e);
		}
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		((SftpPath) path).setAttribute(attribute, value, options);
	}

	void remove(URI path) {
		filesystems.remove(path);
	}

	private int optionsToFlags(Path path, Set<? extends OpenOption> options, SftpPath sftpPath)
			throws FileAlreadyExistsException {
		int flags = 0;
		if(!options.contains(StandardOpenOption.READ) &&
		   !options.contains(StandardOpenOption.WRITE) &&
		   !options.contains(StandardOpenOption.APPEND)) {
			flags |= SftpChannel.OPEN_READ;
		}
		if (options.contains(StandardOpenOption.APPEND)) {
			flags |= SftpChannel.OPEN_APPEND;
		}
		if (options.contains(StandardOpenOption.READ)) {
			flags |= SftpChannel.OPEN_READ;
		}
		if (options.contains(StandardOpenOption.WRITE)) {
			flags |= SftpChannel.OPEN_WRITE;

			if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
				flags |= SftpChannel.OPEN_TRUNCATE;
			}
		}
		if (options.contains(StandardOpenOption.CREATE_NEW)) {
			flags |= SftpChannel.OPEN_CREATE;
			flags |= SftpChannel.OPEN_EXCLUSIVE;
		} else {
			if (options.contains(StandardOpenOption.CREATE)) {
				flags |= SftpChannel.OPEN_CREATE;
			}
		}
		if (options.contains(SftpOpenOption.TEXT)) {
			flags |= SftpChannel.OPEN_TEXT;
		}
		return flags;
	}

}
