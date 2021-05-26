/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;

public interface AbstractFile {

	public abstract String getName();

	public abstract InputStream getInputStream() throws IOException, PermissionDeniedException;

	public abstract boolean exists() throws IOException, PermissionDeniedException;

	public abstract List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException;

	public abstract String getAbsolutePath() throws IOException, PermissionDeniedException;

	public abstract boolean isDirectory() throws IOException, PermissionDeniedException;

	public abstract boolean isFile() throws IOException, PermissionDeniedException;

	public abstract OutputStream getOutputStream() throws IOException, PermissionDeniedException;

	public abstract boolean isHidden() throws IOException, PermissionDeniedException;

	public abstract boolean createFolder() throws PermissionDeniedException, IOException;

	public abstract boolean isReadable() throws IOException, PermissionDeniedException;

	public abstract void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException;

	public abstract void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException;

	public abstract boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException;

	public abstract SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException;

	public abstract void refresh();
	
	long lastModified() throws IOException, PermissionDeniedException;

	long length() throws IOException, PermissionDeniedException;

	boolean isWritable() throws IOException, PermissionDeniedException;

	boolean createNewFile() throws PermissionDeniedException, IOException;
	
	void truncate() throws PermissionDeniedException, IOException;

	void setAttributes(SftpFileAttributes attrs) throws IOException;

	String getCanonicalPath() throws IOException, PermissionDeniedException;
	
	boolean supportsRandomAccess();
	
	AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException;

	OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException;
	
	AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException;
	
	AbstractFileFactory<? extends AbstractFile> getFileFactory();
	
	default void symlinkTo(String target) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

	default String readSymbolicLink() throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
	
}
