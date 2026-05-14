package com.sshtools.common.files.memory;

/*-
 * #%L
 * Base API
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileImpl;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * An in-memory {@link AbstractFile} backed by {@link InMemoryFileFactory}.
 *
 * <p>Supports nested folders and files, random access, append, and all
 * standard SFTP operations. Thread-safe: all mutations synchronize on the
 * owning factory instance.
 */
public class InMemoryFile extends AbstractFileImpl<InMemoryFile> {

	private final String path;   // absolute, normalised
	private final InMemoryFileFactory fs;

	InMemoryFile(String path, InMemoryFileFactory factory) {
		super(factory);
		this.path = path;
		this.fs = factory;
	}

	// -------------------------------------------------------------------------
	// Identity
	// -------------------------------------------------------------------------

	@Override
	public String getName() {
		return InMemoryFileFactory.getName(path);
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return path;
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return path;
	}

	@Override
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		String parentPath = InMemoryFileFactory.getParentPath(path);
		if (parentPath == null) return null;
		return new InMemoryFile(parentPath, fs);
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		if (child == null || child.isEmpty()) return this;
		if (child.startsWith("/")) {
			return new InMemoryFile(InMemoryFileFactory.normalizePath(child), fs);
		}
		return new InMemoryFile(InMemoryFileFactory.normalizePath(path + "/" + child), fs);
	}

	// -------------------------------------------------------------------------
	// Existence / type
	// -------------------------------------------------------------------------

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		return fs.resolve(path) != null;
	}

	@Override
	public boolean isDirectory() throws IOException, PermissionDeniedException {
		InMemoryFileFactory.Node node = fs.resolve(path);
		return node != null && node.directory;
	}

	@Override
	public boolean isFile() throws IOException, PermissionDeniedException {
		InMemoryFileFactory.Node node = fs.resolve(path);
		return node != null && !node.directory;
	}

	@Override
	public boolean isHidden() throws IOException, PermissionDeniedException {
		String name = getName();
		return !name.isEmpty() && name.charAt(0) == '.';
	}

	@Override
	public boolean isReadable() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public boolean isWritable() throws IOException, PermissionDeniedException {
		return true;
	}

	// -------------------------------------------------------------------------
	// Listing
	// -------------------------------------------------------------------------

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) throw new FileNotFoundException(path);
			if (!node.directory) throw new IOException("Not a directory: " + path);
			List<AbstractFile> result = new ArrayList<>();
			for (String name : node.children.keySet()) {
				String childPath = "/".equals(path) ? "/" + name : path + "/" + name;
				result.add(new InMemoryFile(childPath, fs));
			}
			return Collections.unmodifiableList(result);
		}
	}

	// -------------------------------------------------------------------------
	// I/O
	// -------------------------------------------------------------------------

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) throw new FileNotFoundException(path);
			if (node.directory) throw new IOException("Is a directory: " + path);
			return new ByteArrayInputStream(node.content.clone());
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) {
				node = fs.createFile(path);
			} else if (node.directory) {
				throw new IOException("Is a directory: " + path);
			}
			final InMemoryFileFactory.Node target = node;
			return new ByteArrayOutputStream() {
				@Override
				public void close() throws IOException {
					super.close();
					synchronized (fs) {
						target.content = toByteArray();
						target.lastModified = System.currentTimeMillis();
					}
				}
			};
		}
	}

	@Override
	public boolean supportsRandomAccess() {
		return true;
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) {
				if (writeAccess) {
					node = fs.createFile(path);
				} else {
					throw new FileNotFoundException(path);
				}
			}
			if (node.directory) throw new IOException("Is a directory: " + path);
			return new InMemoryFileRandomAccess(node, writeAccess, fs);
		}
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) throw new FileNotFoundException(path);
			if (node.directory) throw new IOException("Is a directory: " + path);
			node.content = new byte[0];
			node.lastModified = System.currentTimeMillis();
		}
	}

	// -------------------------------------------------------------------------
	// Mutation
	// -------------------------------------------------------------------------

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		synchronized (fs) {
			if (fs.resolve(path) != null) return false;
			fs.ensureDir(path);
			return true;
		}
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		synchronized (fs) {
			if (fs.resolve(path) != null) return false;
			fs.createFile(path);
			return true;
		}
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) return false;
			if (node.directory && !node.children.isEmpty() && !recursive) {
				throw new IOException("Directory not empty: " + path);
			}
			return fs.delete(path);
		}
	}

	// -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	@Override
	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) throw new FileNotFoundException(path);
			int type = node.directory
					? SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY
					: SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
			SftpFileAttributesBuilder bldr = SftpFileAttributesBuilder.ofType(type, "UTF-8");
			bldr.withLastModifiedTime(FileTime.fromMillis(node.lastModified));
			if (!node.directory) {
				bldr.withSize(new UnsignedInteger64(node.content.length));
			}
			return bldr.build();
		}
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null) return;
			node.lastModified = attrs.lastModifiedTime().toMillis();
		}
	}

	@Override
	public long lastModified() throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			return node != null ? node.lastModified : 0L;
		}
	}

	@Override
	public long length() throws IOException, PermissionDeniedException {
		synchronized (fs) {
			InMemoryFileFactory.Node node = fs.resolve(path);
			if (node == null || node.directory) return 0L;
			return node.content.length;
		}
	}

	@Override
	public void refresh() {
		// No caching; always reads live from node tree.
	}

	// -------------------------------------------------------------------------
	// AbstractFileImpl requirements
	// -------------------------------------------------------------------------

	@Override
	protected int doHashCode() {
		return path.hashCode();
	}

	@Override
	protected boolean doEquals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof InMemoryFile)) return false;
		return path.equals(((InMemoryFile) obj).path);
	}

	@Override
	public String toString() {
		return path;
	}
}
