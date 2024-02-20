package com.sshtools.common.files.direct;

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
		if(attrs.hasLastModifiedTime()) {
			f.setLastModified(attrs.lastModifiedTime().toMillis());
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
