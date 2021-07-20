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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.FileUtils;

public class VirtualMountFile extends VirtualFileObject {

	private VirtualMount mount;
	private String name;
	private String path;
	private AbstractFile file;
	
	Map<String,AbstractFile> cachedChildren;
	
	public VirtualMountFile(String path, VirtualMount mount, VirtualFileFactory fileFactory) throws PermissionDeniedException, IOException {
		super(fileFactory);
		
		this.mount = mount;

		int idx = path.lastIndexOf('/');
		if(idx > -1) {
			name = path.substring(idx+1);
		} else {
			name = path;
		}
		this.path = path;
	}
	
	private AbstractFile resolveFile() throws PermissionDeniedException, IOException {
		if(Objects.nonNull(file)) {
			return file;
		}
		return file = mount.getActualFileFactory().getFile(mount.getResolvePath(path));
	}
	
	public boolean exists() throws IOException, PermissionDeniedException {
		return resolveFile().exists();
	}

	public boolean createFolder() throws PermissionDeniedException, IOException {
		return resolveFile().createFolder();
	}

	public long lastModified() throws IOException, PermissionDeniedException {
		return resolveFile().lastModified();
	}

	public String getName() {
		return name;
	}

	public long length() throws IOException, PermissionDeniedException {
		return resolveFile().length();
	}

	public SftpFileAttributes getAttributes() throws FileNotFoundException,
			IOException, PermissionDeniedException {
		return resolveFile().getAttributes();
	}

	public boolean isHidden() throws IOException, PermissionDeniedException {
		return resolveFile().isHidden();
	}

	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return resolveFile().isDirectory();
	}

	public synchronized List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException {

		if(Objects.isNull(cachedChildren)) {
			
			Map<String,AbstractFile> files = new HashMap<>(getVirtualMounts());
			
			String currentPath = FileUtils.checkEndsWithSlash(path);
	
			VirtualMountManager mgr = fileFactory.getMountManager();
		
			/**
			 * LDP - Here we are merging a potential real path from a lower mount. Check that the file
			 * really exists as we do not want to generate an error here.
			 */
			AbstractFile file = resolveFile();
			if(file.exists()) {
				VirtualMount actualMount = mgr.getMount(currentPath);
				for(AbstractFile child : file.getChildren()) {
					files.put(currentPath + child.getName(), new VirtualMountFile(currentPath + child.getName(), actualMount, fileFactory));
				}
			}
			
			cachedChildren = files;
		}
		
		return new ArrayList<>(cachedChildren.values());
		

	}

	public boolean isFile() throws IOException, PermissionDeniedException {
		return resolveFile().isFile();
	}

	public String getAbsolutePath() throws IOException,
			PermissionDeniedException {
		return path;
	}

	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		return resolveFile().getInputStream();
	}

	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		return resolveFile().getOutputStream();
	}

	public boolean isReadable() throws IOException, PermissionDeniedException {
		return resolveFile().isReadable();
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
		return resolveFile().delete(recursive);
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
		return resolveFile().isWritable();
	}

	public boolean createNewFile() throws PermissionDeniedException,
			IOException {
		return resolveFile().createNewFile();
	}

	public void truncate() throws PermissionDeniedException, IOException {
		resolveFile().truncate();
	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		file.setAttributes(attrs);
	}

	public String getCanonicalPath() throws IOException,
			PermissionDeniedException {
		return path;
	}

	public boolean supportsRandomAccess() {
		try {
			return resolveFile().supportsRandomAccess();
		} catch (PermissionDeniedException | IOException e) {
			return false;
		}
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess)
			throws IOException, PermissionDeniedException {
		return resolveFile().openFile(writeAccess);
	}

	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		return resolveFile().getOutputStream(append);
	}

	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		if(child.startsWith("/")) {
			return fileFactory.getFile(child);
		} else {
			return fileFactory.getFile(path + (path.equals("/") || path.endsWith("/") ? "" : "/") + child);
		}
	}

	public AbstractFileFactory<VirtualFile> getFileFactory() {
		return mount.getVirtualFileFactory();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(mount, path);
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
				&& Objects.equals(other.mount, this.mount);
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		resolveFile().symlinkTo(target);
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		return resolveFile().readSymbolicLink();
	}

}
