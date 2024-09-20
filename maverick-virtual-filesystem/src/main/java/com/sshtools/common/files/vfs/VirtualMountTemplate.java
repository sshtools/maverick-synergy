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

import org.apache.commons.vfs2.FileSystemOptions;

import com.sshtools.common.files.AbstractFileFactory;

public class VirtualMountTemplate extends AbstractMount {

	private AbstractFileFactory<?> actualFileFactory;
	private FileSystemOptions fileSystemOptions;
	private boolean createMountFolder;
	private long lastModified = 0;
	
	public VirtualMountTemplate(String mount, String path,
			AbstractFileFactory<?> actualFileFactory,
			boolean createMountFolder) {
		super(mount, path, false, false);
		this.actualFileFactory = actualFileFactory;
		this.createMountFolder = createMountFolder;
	}
	
	public VirtualMountTemplate(String mount, String path,
			AbstractFileFactory<?> actualFileFactory,
			boolean createMountFolder, 
			long lastModified) {
		super(mount, path, false, false);
		this.actualFileFactory = actualFileFactory;
		this.createMountFolder = createMountFolder;
		this.lastModified = lastModified;
	}

	public boolean isCreateMountFolder() {
		return createMountFolder;
	}
	public AbstractFileFactory<?> getActualFileFactory() {
		return actualFileFactory;
	}

	public FileSystemOptions getFileSystemOptions() {
		return fileSystemOptions;
	}

	public void setFileSystemOptions(FileSystemOptions fileSystemOptions) {
		this.fileSystemOptions = fileSystemOptions;
	}
	
	public boolean isParentOf(VirtualMountTemplate o2) {
		return o2.getMount().startsWith(getMount());
	}

	public boolean isChildOf(VirtualMountTemplate o2) {
		return getMount().startsWith(o2.getMount());
	}

	public long lastModified() {
		return lastModified;
	}
	
	public VirtualMountTemplate setUsername(String username) {
		this.username = username;
		return this;
	}

	public VirtualMountTemplate setGroup(String group) {
		this.group = group;
		return this;
	}

	public VirtualMountTemplate setUid(int uid) {
		this.uid = uid;
		return this;
	}

	public VirtualMountTemplate setGid(int gid) {
		this.gid = gid;
		return this;
	}
}
