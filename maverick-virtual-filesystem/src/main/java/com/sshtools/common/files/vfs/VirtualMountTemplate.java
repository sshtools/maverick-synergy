
package com.sshtools.common.files.vfs;

import org.apache.commons.vfs2.FileSystemOptions;

import com.sshtools.common.files.AbstractFileFactory;

public final class VirtualMountTemplate extends AbstractMount {

	private AbstractFileFactory<?> actualFileFactory;
	private FileSystemOptions fileSystemOptions;
	private boolean createMountFolder;
	
	public VirtualMountTemplate(String mount, String path,
			AbstractFileFactory<?> actualFileFactory,
			boolean createMountFolder) {
		super(mount, path, false, false);
		this.actualFileFactory = actualFileFactory;
		this.createMountFolder = createMountFolder;
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
}
