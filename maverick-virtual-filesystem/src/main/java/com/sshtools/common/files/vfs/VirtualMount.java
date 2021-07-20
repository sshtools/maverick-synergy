

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
	
	VirtualMount(String mount, String path,
			VirtualFileFactory virtualFileFactory,
			AbstractFileFactory<?> actualFileFactory,
			boolean isDefault,
			boolean isImaginary, boolean createMountFolder)
				throws IOException, PermissionDeniedException {
		super(mount, path, isDefault, isImaginary);
		this.actualFileFactory = actualFileFactory;
		this.virtualFileFactory = virtualFileFactory;
		this.createMountFolder = createMountFolder;
		if (!isImaginary()) {
			AbstractFile f = actualFileFactory.getFile(path);
			this.path = f.getAbsolutePath();
		}

	}

	public VirtualMount(String mount, String path,
			VirtualFileFactory virtualFileFactory,
			AbstractFileFactory<?> actualFileFactory,
			boolean createMountFolder) throws IOException,
			PermissionDeniedException {
		this(mount, path, virtualFileFactory, actualFileFactory, false,
				false, createMountFolder);
	}

	public AbstractFileFactory<? extends AbstractFile> getActualFileFactory() {
		return actualFileFactory;
	}

	public String getResolvePath(String path) {
		if (path.length() > FileUtils.addTrailingSlash(mount).length()) {
			return FileUtils.addTrailingSlash(this.path)
					+ path.substring(FileUtils.addTrailingSlash(mount)
							.length());
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
}
