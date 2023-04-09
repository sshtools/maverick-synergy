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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.Multipart;
import com.sshtools.common.sftp.MultipartTransfer;
import com.sshtools.common.sftp.SftpFileAttributes;

public class AbstractFileAdapter implements AbstractFile {

	protected AbstractFile file;
	
	public AbstractFileAdapter(AbstractFile file) {
		this.file = file;
	}
	
	public AbstractFileAdapter() {
	}
	
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		return file.getParentFile();
	}
	
	protected void init(AbstractFile file) {
		this.file = file;
	}
	
	public boolean exists() throws IOException, PermissionDeniedException {
		return file.exists();
	}

	public boolean createFolder() throws IOException, PermissionDeniedException {
		return file.createFolder();
	}

	public long lastModified() throws IOException, PermissionDeniedException{
		return file.lastModified();
	}

	public String getName() {
		return file.getName();
	}

	public long length() throws IOException, PermissionDeniedException {
		return file.length();
	}

	public SftpFileAttributes getAttributes() throws IOException, PermissionDeniedException {
		return file.getAttributes();
	}

	public boolean isDirectory() throws IOException, PermissionDeniedException {
		return file.isDirectory();
	}

	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return file.getChildren();
	}

	public boolean isFile() throws IOException, PermissionDeniedException {
		return file.isFile();
	}

	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return file.getAbsolutePath();
	}

	public boolean isReadable() throws IOException, PermissionDeniedException {
		return file.isReadable();
	}

	public boolean createNewFile() throws PermissionDeniedException,
			IOException {
		return file.createNewFile();
	}

	public void truncate() throws PermissionDeniedException, IOException {
		file.truncate();
	}

	public InputStream getInputStream() throws IOException, PermissionDeniedException {
		return file.getInputStream();
	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		file.setAttributes(attrs);
	}

	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return file.getCanonicalPath();
	}

	public boolean supportsRandomAccess() {
		return file.supportsRandomAccess();
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException, PermissionDeniedException {
		return file.openFile(writeAccess);
	}

	public boolean isHidden() throws IOException, PermissionDeniedException {
		return file.isHidden();
	}

	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		return file.getOutputStream();
	}

	public boolean isWritable() throws IOException, PermissionDeniedException {
		return file.isWritable();
	}

	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException {
		file.copyFrom(src);		
	}

	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {
		file.moveTo(target);
	}

	public boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException {
		return file.delete(recursive);
		
	}

	public void refresh() {
		if(file!=null) {
			file.refresh();
		}
	}

	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		return file.getOutputStream(append);
	}

	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		return file.resolveFile(child);
	}

	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return file.getFileFactory();
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		file.symlinkTo(target);
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		return file.readSymbolicLink();
	}
	
	@Override
	public boolean supportsMultipartTransfers() {
		return file.supportsMultipartTransfers();
	}

	@Override
	public MultipartTransfer startMultipartUpload(Collection<Multipart> multparts) throws IOException, PermissionDeniedException {
		return file.startMultipartUpload(multparts);
	}

}
