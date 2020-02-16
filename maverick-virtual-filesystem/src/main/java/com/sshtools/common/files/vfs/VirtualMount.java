/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
/* HEADER */
package com.sshtools.common.files.vfs;

import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.FileUtils;

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
}
