/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.files.vfs;

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
}
