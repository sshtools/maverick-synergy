/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
