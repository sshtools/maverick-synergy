package com.sshtools.synergy.s3;

/*-
 * #%L
 * S3 File System
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.FileUtils;

import software.amazon.awssdk.services.s3.model.CommonPrefix;

public class S3AbstractFolder implements S3File {

	S3BucketFile bucket;
	CommonPrefix prefix;
	S3AbstractFileFactory factory;
	
	S3AbstractFolder(S3AbstractFileFactory factory, S3BucketFile bucket, CommonPrefix prefix) {
		this.factory = factory;
		this.bucket = bucket;
		this.prefix = prefix;
	}
	
	@Override
	public String getName() {
		return FileUtils.lastPathElement(FileUtils.checkEndsWithNoSlash(prefix.prefix()));
	}

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("InputStream is not available on a folder object");
	}

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return factory.resolveChildren(bucket, prefix.prefix());
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return bucket.getAbsolutePath() + prefix.prefix();
	}

	@Override
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		return factory.resolveFile(bucket, FileUtils.stripLastPathElement(prefix.prefix()));
	}

	@Override
	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public boolean isFile() throws IOException, PermissionDeniedException {
		return false;
	}

	@Override
	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("OutputStream is not available on a folder object");
	}

	@Override
	public boolean isHidden() throws IOException, PermissionDeniedException {
		return false;
	}

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		return false;
	}

	@Override
	public boolean isReadable() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		return false;
	}

	@Override
	public void refresh() {
		
	}

	@Override
	public long lastModified() throws IOException, PermissionDeniedException {
		return bucket.lastModified();
	}

	@Override
	public long length() throws IOException, PermissionDeniedException {
		return 0;
	}

	@Override
	public boolean isWritable() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		throw new PermissionDeniedException("You cannot create file on a folder object");
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return getAbsolutePath();
	}

	@Override
	public boolean supportsRandomAccess() {
		return false;
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("Cannot open a folder object");
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("OutputStream is not available on a folder object");
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		return factory.resolveFile(bucket, child);
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return factory;
	}

}
