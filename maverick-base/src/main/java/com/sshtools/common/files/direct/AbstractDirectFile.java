/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
import com.sshtools.common.ssh.SshConnection;

public abstract class AbstractDirectFile<T extends AbstractDirectFile<T>> extends AbstractFileImpl<T> {

	protected File f;
	protected String homeDir;
	protected boolean hidden = false;
	
	public AbstractDirectFile(String path, AbstractFileFactory<T> fileFactory, SshConnection con, String homeDir) throws IOException {
		super(fileFactory, con);
		this.con = con;
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
		return f.mkdir();
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

	public void setAttributes(SftpFileAttributes attrs) {
		
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
}
