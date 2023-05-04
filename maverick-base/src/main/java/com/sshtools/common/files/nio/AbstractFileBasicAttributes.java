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
