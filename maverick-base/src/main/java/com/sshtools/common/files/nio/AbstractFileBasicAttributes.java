package com.sshtools.common.files.nio;

/*-
 * #%L
 * Base API
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
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

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
			return e.getAttributes().createTime();
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
		} catch (IOException | PermissionDeniedException e) {
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
		} catch (IOException | PermissionDeniedException e) {
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
			return e.getAttributes().lastAccessTime();
		} catch (IOException | PermissionDeniedException e) {
			return null;
		}
	}

	@Override
	public FileTime lastModifiedTime() {
		try {
			return e.getAttributes().lastModifiedTime();
		} catch (IOException | PermissionDeniedException e) {
			return null;
		}
	}

	@Override
	public long size() {
		try {
			return e.getAttributes().size().longValue();
		} catch (IOException | PermissionDeniedException e) {
			return 0;
		}
	}
}
