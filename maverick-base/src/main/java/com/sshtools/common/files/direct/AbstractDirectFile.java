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
package com.sshtools.common.files.direct;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileImpl;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.files.RandomAccessImpl;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;

public abstract class AbstractDirectFile<T extends AbstractDirectFile<T>> extends AbstractFileImpl<T> {

	protected File f;
	protected File homeDir;
	protected boolean hidden = false;
	
	public AbstractDirectFile(String path, AbstractFileFactory<T> fileFactory, File homeDir) throws IOException {
		super(fileFactory);
		this.homeDir = homeDir;
		f = new File(path);
		if(!f.isAbsolute())
			f = new File(homeDir, path);
		
		if(f.exists()) {
			getAttributes();
		}
		
		hidden = f.getName().startsWith(".");
	}
	
	public boolean exists() {
		return f.exists();
	}

	public boolean createFolder() throws PermissionDeniedException {
		return f.mkdirs();
	}

	public long lastModified() {
		return f.lastModified();
	}

	public String getName() {
		return f.getName();
	}

	public long length() {
		return f.length();
	}

	public abstract SftpFileAttributes getAttributes() throws IOException;

	public boolean isDirectory() {
		return f.isDirectory();
	}

	public boolean isFile() {
		return f.isFile();
	}

	public String getAbsolutePath() throws IOException {
		return f.getAbsolutePath();
	}

	public boolean isReadable() {
		return true;
	}

	public boolean isWritable() {
		return true;
	}

	public boolean createNewFile() throws PermissionDeniedException, IOException {
		return f.createNewFile();
	}

	public void truncate() throws PermissionDeniedException, IOException {
		f.delete();
		f.createNewFile();
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(f);
	}

	public OutputStream getOutputStream() throws IOException {
		return new FileOutputStream(f);
	}

	public boolean delete(boolean recurse) {
		return f.delete();
	}

	public void moveTo(AbstractFile f2) throws IOException, PermissionDeniedException {
		f.renameTo(new File(f2.getAbsolutePath()));
	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		
		if(attrs.hasModifiedTime()) {
			f.setLastModified(attrs.getModifiedTime().longValue() * 1000);
		}

	}

	public String getCanonicalPath() throws IOException {
		return f.getCanonicalPath();
	}

	public boolean supportsRandomAccess() {
		return true;
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException {
		return new RandomAccessImpl(f, writeAccess);
	}


	public boolean isHidden() {
		return hidden;
	}

	public void refresh() {
		
	}
	

	@Override
	protected int doHashCode() {
		return f.hashCode();
	}

	@Override
	protected boolean doEquals(Object obj) {
		if(obj instanceof AbstractDirectFile) {
			AbstractDirectFile<?> f2 = (AbstractDirectFile<?>) obj;
			return f.equals(f2.f);
		}
		return false;
	}
}
