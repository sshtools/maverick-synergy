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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.util.FileUtils;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

public class S3BucketFile implements S3File {

	S3Client s3;
	Bucket bucket;
	S3AbstractFileFactory factory;
	
	public S3BucketFile(S3AbstractFileFactory factory, S3Client s3, Bucket bucket) {
		this.s3 = s3;
		this.bucket = bucket;
		this.factory = factory;
	}

	@Override
	public String getName() {
		return bucket.name();
	}

	@Override
	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("You cannot open an InputStream on a bucket!");
	}

	@Override
	public boolean exists() throws IOException, PermissionDeniedException {
		return true;
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return factory.resolveChildren(this, getAbsolutePath());
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return FileUtils.checkEndsWithSlash(FileUtils.checkStartsWithSlash(bucket.name()));
	}

	@Override
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("A bucket does not have a parent file");
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
		throw new PermissionDeniedException("You cannot open an OutputStream on a bucket!");
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
		
		try {
			s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket.name()).build());
			return true;
		} catch (AwsServiceException | SdkClientException e) {
			Log.error("Failed to delete bucket {}", bucket.name(), e);
			return false;
		}
	}

	@Override
	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		
		return SftpFileAttributesBuilder.create()
			.withCharsetEncoding("UTF-8")
			.withLastModifiedTime(bucket.creationDate().toEpochMilli())
			.withGid(0)
			.withGroup(System.getProperty("maverick.unknownUsername", "unknown"))
			.withUid(0)
			.withUsername(System.getProperty("maverick.unknownUsername", "unknown"))
			.withSize(0L)
			.withType(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY)
			.withPermissions(PosixPermissionsBuilder.create().fromBitmask(0777).build())
			.build();

	}

	@Override
	public void refresh() {

	}
	
	@Override
	public long lastModified() throws IOException, PermissionDeniedException {
		return bucket.creationDate().toEpochMilli();
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
		throw new PermissionDeniedException("You cannot create a file; this object is a bucket!");
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		throw new PermissionDeniedException("You cannot truncate a bucket!");
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
		throw new PermissionDeniedException("You cannot open a bucket!");
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		throw new PermissionDeniedException("You cannot open an OutputStream on a bucket!");
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		return factory.resolveFile(this, child);
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return factory;
	}

}
