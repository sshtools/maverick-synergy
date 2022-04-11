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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.files.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger64;

public class VirtualMountFile extends VirtualFileObject {

	private String name;
	private String path;
	private AbstractFile file;
	private boolean intermediate;
	
	Map<String,VirtualFile> cachedChildren;
	
	public VirtualMountFile(String path, VirtualMount mount, VirtualFileFactory fileFactory, boolean intermediate) throws PermissionDeniedException, IOException {
		super(fileFactory, mount);
		path = FileUtils.checkEndsWithNoSlash(path);
		int idx = path.lastIndexOf('/');
		if(idx > -1) {
			name = path.substring(idx+1);
		} else {
			name = path;
		}
		this.path = path;
		this.intermediate = intermediate;
	}
	
	public boolean isMount() {
		return true;
	}
	
	public AbstractFile resolveFile() throws PermissionDeniedException, IOException {
		if(Objects.nonNull(file)) {
			return file;
		}
		return file = parentMount.getActualFileFactory().getFile(parentMount.getResolvePath(path));
	}
	
	public boolean exists() throws IOException, PermissionDeniedException {
		return true;
	}

	public boolean createFolder() throws PermissionDeniedException, IOException {
		return false;
	}

	public long lastModified() throws IOException, PermissionDeniedException {
		return parentMount.lastModified();
	}

	public String getName() {
		return name;
	}

	public long length() throws IOException, PermissionDeniedException {
		return 0;
	}

	public SftpFileAttributes getAttributes() throws FileNotFoundException,
			IOException, PermissionDeniedException {
		
		SftpFileAttributes attrs = new SftpFileAttributes(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY, "UTF-8");
		attrs.setPermissions(parentMount.defaultPermissions());
		try {
			attrs.setReadOnly(parentMount.isReadOnly());
		} catch (SftpStatusException e) {
		}
		attrs.setTimes(new UnsignedInteger64(parentMount.lastModified()),
				new UnsignedInteger64(parentMount.lastModified()),
				new UnsignedInteger64(parentMount.lastModified()));
		return attrs;
		
	}

	public boolean isHidden() throws IOException, PermissionDeniedException {
		return false;
	}

	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return isMount();
	}

	public synchronized List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException {

		if(Objects.isNull(cachedChildren)) {
			cachedChildren = fileFactory.resolveChildren(this);
		}
		
		return new ArrayList<>(cachedChildren.values());
		

	}

	public boolean isFile() throws IOException, PermissionDeniedException {
		return false;
	}

	public String getAbsolutePath() throws IOException,
			PermissionDeniedException {
		return path;
	}

	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		throw new IOException("No I/O stream supported on non-file");
	}

	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		throw new IOException("No I/O stream supported on non-file");
	}

	public boolean isReadable() throws IOException, PermissionDeniedException {
		return isMount();
	}

	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException {
		resolveFile().copyFrom(src);
	}

	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {
		resolveFile().moveTo(target);
	}

	public boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException {
		throw new PermissionDeniedException("You cannot delete a mounted folder object");
	}

	public synchronized void refresh() {
		try {
			cachedChildren = null;
			super.refresh();
			resolveFile().refresh();
		} catch (PermissionDeniedException | IOException e) {
			// Purposely ignored
		}
	}

	public boolean isWritable() throws IOException, PermissionDeniedException {
		return !intermediate && (!parentMount.isReadOnly() || resolveFile().isWritable());
	}

	public boolean createNewFile() throws PermissionDeniedException,
			IOException {
		throw new PermissionDeniedException("You cannot create a mounted folder object");
	}

	public void truncate() throws PermissionDeniedException, IOException {
		throw new PermissionDeniedException("You cannot truncate a mounted folder object");
	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		try {
			if(!isWritable()) {
				throw new IOException("Mount is not writable");
			}
		} catch (PermissionDeniedException e) {
			throw new IOException(e.getMessage(), e);
		}
		file.setAttributes(attrs);
	}

	public String getCanonicalPath() throws IOException,
			PermissionDeniedException {
		return path;
	}

	public boolean supportsRandomAccess() {
		return false;
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess)
			throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("You cannot open a mounted folder object");
	}

	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("You cannot open a stream on a mounted folder object");
	}

	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		if(child.startsWith("/")) {
			return fileFactory.getFile(child);
		} else {
			return fileFactory.getFile(path + (path.equals("/") || path.endsWith("/") ? "" : "/") + child);
		}
	}

	public AbstractFileFactory<VirtualFile> getFileFactory() {
		return parentMount.getVirtualFileFactory();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(parentMount, path);
	}

	@Override
	public boolean equals(Object obj) {
		
		if(obj == this) {
			return true;
		}
		if(Objects.isNull(obj)) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		VirtualMountFile other = getClass().cast(obj);
		return Objects.equals(other.path, this.path)
				&& Objects.equals(other.parentMount, this.parentMount);
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("Cannot symlink a mount");
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		return getAbsolutePath();
	}

}
