/* HEADER */
package com.sshtools.common.files.vfs;

import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.FileSystemUtils;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class VirtualMount extends AbstractMount {

	AbstractFileFactory<? extends AbstractFile> actualFileFactory;
	VirtualFileFactory virtualFileFactory;
	boolean cached;

	VirtualMount(String mount, String path,
			VirtualFileFactory virtualFileFactory,
			AbstractFileFactory<?> actualFileFactory,
			SshConnection con, boolean isDefault,
			boolean isImaginary) throws IOException, PermissionDeniedException {
		super(mount, path, isDefault, isImaginary);
		this.actualFileFactory = actualFileFactory;
		this.virtualFileFactory = virtualFileFactory;
		if (!isImaginary()) {
			AbstractFile f = actualFileFactory.getFile(path, con);
			if (!f.isFile()) {
				f.createFolder();
			}
			this.path = f.getAbsolutePath();
		}

	}

	public VirtualMount(String mount, String path,
			VirtualFileFactory virtualFileFactory,
			AbstractFileFactory<?> actualFileFactory,
			SshConnection con) throws IOException,
			PermissionDeniedException {
		this(mount, path, virtualFileFactory, actualFileFactory, con, false,
				false);
	}

	public AbstractFileFactory<? extends AbstractFile> getActualFileFactory() {
		return actualFileFactory;
	}

	public String getResolvePath(String path) {
		if (path.length() > FileSystemUtils.addTrailingSlash(mount).length()) {
			return FileSystemUtils.addTrailingSlash(this.path)
					+ path.substring(FileSystemUtils.addTrailingSlash(mount)
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
}
