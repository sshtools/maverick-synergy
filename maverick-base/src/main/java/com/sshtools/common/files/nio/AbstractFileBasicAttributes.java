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
package com.sshtools.common.files.nio;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;

public class AbstractFileBasicAttributes implements BasicFileAttributes {
	private final AbstractFile e;

	AbstractFileBasicAttributes(AbstractFile e) {
		this.e = e;
	}

	@Override
	public FileTime creationTime() {
		try {
			return FileTime.from(e.getAttributes().getCreationTime().longValue(), TimeUnit.SECONDS);
		} catch (IOException | PermissionDeniedException e) {
			return null;
		}
	}

	@Override
	public Object fileKey() {
		return null;
	}

	@Override
	public boolean isDirectory() {
		try {
			return e.isDirectory();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public boolean isRegularFile() {
		try {
			return e.isFile();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public FileTime lastAccessTime() {
		try {
			return FileTime.from(e.getAttributes().getAccessedTime().longValue(), TimeUnit.SECONDS);
		} catch (IOException | PermissionDeniedException e) {
			return null;
		}
	}

	@Override
	public FileTime lastModifiedTime() {
		try {
			return FileTime.from(e.getAttributes().getModifiedTime().longValue(), TimeUnit.SECONDS);
		} catch (IOException | PermissionDeniedException e) {
			return null;
		}
	}

	@Override
	public long size() {
		try {
			return e.getAttributes().getSize().longValue();
		} catch (IOException | PermissionDeniedException e) {
			return 0;
		}
	}
}
