package com.sshtools.common.files.vfs;

/*-
 * #%L
 * Virtual File System
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

import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VirtualMount extends AbstractMount {

	AbstractFileFactory<? extends AbstractFile> actualFileFactory;
	VirtualFileFactory virtualFileFactory;
	boolean cached;
	boolean createMountFolder;
	boolean readOnly;
	long lastModified = 0;
	VirtualMountTemplate mountTemplate;
	
	VirtualMount(VirtualMountTemplate mountTemplate, 
			VirtualFileFactory virtualFileFactory,
			AbstractFileFactory<?> actualFileFactory,
			boolean isDefault,
			boolean isImaginary, 
			boolean createMountFolder,
			long lastModified)
				throws IOException, PermissionDeniedException {
		super(mountTemplate.getMount(), 
				mountTemplate.getRoot(),
				isDefault, isImaginary);

		this.mountTemplate = mountTemplate;
		this.actualFileFactory = actualFileFactory;
		this.virtualFileFactory = virtualFileFactory;
		this.createMountFolder = createMountFolder;
		this.lastModified = lastModified;
		if (!isImaginary()) {
			AbstractFile f = actualFileFactory.getFile(path);
			this.path = f.getAbsolutePath();
		}
		
		this.uid = mountTemplate.getUid();
		this.gid = mountTemplate.getGid();
		this.username = mountTemplate.getUsername();
		this.group = mountTemplate.getGroup();

	}

	public VirtualMount(VirtualMountTemplate mountTemplate, 
			VirtualFileFactory virtualFileFactory,
			AbstractFileFactory<?> actualFileFactory,
			boolean createMountFolder, long lastModified) throws IOException,
			PermissionDeniedException {
		this(mountTemplate, virtualFileFactory, actualFileFactory, false,
				false, createMountFolder, lastModified);
	}

	public VirtualMountTemplate getTemplate() {
		return mountTemplate;
	}
	
	public AbstractFileFactory<? extends AbstractFile> getActualFileFactory() {
		return actualFileFactory;
	}

	public String getResolvePath(String path) {
		
		String thisMount = FileUtils.addTrailingSlash(mount);
		String thisPath = FileUtils.addTrailingSlash(path);
		if (thisPath.length() >= thisMount.length()) {
			return FileUtils.addTrailingSlash(this.path)
					+ thisPath.substring(thisMount.length());
		} else {
			return this.path;
		}

	}

	public boolean isCached() {
		return cached;
	}

	public void setCached(boolean cached) {
		this.cached = cached;
	}

	public AbstractFileFactory<VirtualFile> getVirtualFileFactory() {
		return virtualFileFactory;
	}

	public boolean isCreateMountFolder() {
		return createMountFolder;
	}

	public boolean isParentOf(VirtualMount o2) {
		return o2.getMount().startsWith(getMount());
	}

	public boolean isChildOf(VirtualMount o2) {
		return getMount().startsWith(o2.getMount());
	}

	public long lastModified() {
		return lastModified;
	}
	
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
	
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
}
