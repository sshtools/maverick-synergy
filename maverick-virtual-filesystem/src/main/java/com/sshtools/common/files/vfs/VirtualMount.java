package com.sshtools.common.files.vfs;

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
