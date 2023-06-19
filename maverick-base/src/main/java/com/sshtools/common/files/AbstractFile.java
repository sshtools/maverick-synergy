/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.MultipartTransfer;
import com.sshtools.common.sftp.OpenFile;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.files.PseduoRandomOpenFile;
import com.sshtools.common.sftp.files.RandomAccessOpenFile;
import com.sshtools.common.util.UnsignedInteger32;

public interface AbstractFile {

	String getName();

	InputStream getInputStream() throws IOException, PermissionDeniedException;

	boolean exists() throws IOException, PermissionDeniedException;

	default boolean existsNoFollowLinks() throws IOException, PermissionDeniedException {
		return exists();
	}

	List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException;

	String getAbsolutePath() throws IOException, PermissionDeniedException;

	AbstractFile getParentFile() throws IOException, PermissionDeniedException;
	
	boolean isDirectory() throws IOException, PermissionDeniedException;

	boolean isFile() throws IOException, PermissionDeniedException;

	OutputStream getOutputStream() throws IOException, PermissionDeniedException;

	boolean isHidden() throws IOException, PermissionDeniedException;

	boolean createFolder() throws PermissionDeniedException, IOException;

	boolean isReadable() throws IOException, PermissionDeniedException;

	boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException;

	SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException;

	default SftpFileAttributes getAttributesNoFollowLinks() throws FileNotFoundException, IOException, PermissionDeniedException {
		return getAttributes();
	}

	void refresh();
	
	long lastModified() throws IOException, PermissionDeniedException;

	long length() throws IOException, PermissionDeniedException;

	boolean isWritable() throws IOException, PermissionDeniedException;

	boolean createNewFile() throws PermissionDeniedException, IOException;
	
	void truncate() throws PermissionDeniedException, IOException;

	void setAttributes(SftpFileAttributes attrs) throws IOException;

	String getCanonicalPath() throws IOException, PermissionDeniedException;
	
	boolean supportsRandomAccess();
	
	default OpenFile open(UnsignedInteger32 flags, Optional<UnsignedInteger32> accessFlags, byte[] handle) throws IOException, PermissionDeniedException {
		if(supportsRandomAccess()) {
			return new RandomAccessOpenFile(this, flags, handle);
		} else {
			return new PseduoRandomOpenFile(this, flags, handle);
		}
	}
	
	AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException;

	OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException;
	
	AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException;
	
	AbstractFileFactory<? extends AbstractFile> getFileFactory();
	
	@Deprecated(since = "3.1.0",  forRemoval = true)
	default void symlinkTo(String target) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
	
	default void symlinkFrom(String target) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

	@Deprecated(since = "3.1.0",  forRemoval = true)
	default void linkTo(String target) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

	default void linkFrom(String target) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

	default String readSymbolicLink() throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
	
	default boolean supportsMultipartTransfers() {
		return false;
	}

	default MultipartTransfer startMultipartUpload(AbstractFile targetFile) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
	
	default void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {

		if(src.isDirectory()) {
			createFolder();
			for(var f : src.getChildren()) {
				resolveFile(f.getName()).copyFrom(f);
			}
		} else if(src.isFile()) {
			try(var in = src.getInputStream()) {
				try(var out = getOutputStream()) {
					in.transferTo(out);
				}
			}
		} else {
			throw new IOException("Cannot copy object that is not directory or a regular file");
		}
	
	}

	default void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {

		if(isDirectory()) {
			target.createFolder();
			for(var f : getChildren()) {
				target.resolveFile(f.getName()).copyFrom(f);
				f.delete(false);
			}
		} else if(isFile()) {
			try(var in = getInputStream()) {
				try(var out = target.getOutputStream()) {
					in.transferTo(out);
				}
			}
		} else {
			throw new IOException("Cannot move object that is not directory or a regular file");
		}
		
		delete(false);
	
	}
	
	default FileVolume getVolume() throws IOException {
		throw new UnsupportedOperationException("File storage information is not available on this file system.");
	}
}
