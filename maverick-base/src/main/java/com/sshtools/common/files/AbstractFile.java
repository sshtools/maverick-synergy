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

package com.sshtools.common.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.Multipart;
import com.sshtools.common.sftp.MultipartTransfer;
import com.sshtools.common.sftp.SftpFileAttributes;

public interface AbstractFile {

	public String getName();

	public InputStream getInputStream() throws IOException, PermissionDeniedException;

	public boolean exists() throws IOException, PermissionDeniedException;

	public List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException;

	public String getAbsolutePath() throws IOException, PermissionDeniedException;

	public AbstractFile getParentFile() throws IOException, PermissionDeniedException;
	
	public boolean isDirectory() throws IOException, PermissionDeniedException;

	public boolean isFile() throws IOException, PermissionDeniedException;

	public OutputStream getOutputStream() throws IOException, PermissionDeniedException;

	public boolean isHidden() throws IOException, PermissionDeniedException;

	public boolean createFolder() throws PermissionDeniedException, IOException;

	public boolean isReadable() throws IOException, PermissionDeniedException;

	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException;

	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException;

	public boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException;

	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException;

	public void refresh();
	
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
	
	default boolean supportsMultipartTransfers() {
		return false;
	}

	default MultipartTransfer startMultipartUpload(Collection<Multipart> multparts) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
	
}
