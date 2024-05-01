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
