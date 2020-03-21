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
package com.sshtools.common.files.memory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger64;

public class InMemoryAbstractFile implements AbstractFile {

	private String path;
	private InMemoryFileSystem fs;
    private SftpFileAttributes attrs;
    private AbstractFileFactory<InMemoryAbstractFile> fileFactory;
    
    public InMemoryAbstractFile(String path, InMemoryFileSystem fs, AbstractFileFactory<InMemoryAbstractFile> fileFactory) {
    	this.path = path;
    	this.fs = fs;
    	this.fileFactory = fileFactory;
	}
	
	@Override
	public String getName() {
		return FileUtils.getFilename(path);
	}
	
	private InMemoryFile getFile() throws IOException {
		return fs.getFile(path);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return getFile().getInputStream();
	}

	@Override
	public boolean exists() throws IOException {
		return fs.exists(path);
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return getFile().getChildren().stream().map((fo) -> {
			return new InMemoryAbstractFile(fo.getPath(), fs, InMemoryAbstractFile.this.fileFactory);
		}).collect(Collectors.toList());
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return path;
	}

	@Override
	public boolean isDirectory() throws IOException {
		return getFile().isFolder();
	}

	@Override
	public boolean isFile() throws IOException {
		return getFile().isFile();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if(!exists()) {
			try {
				createNewFile();
			} catch (PermissionDeniedException e) {
				throw new IOException(e);
			}
		}
		return getFile().getOutputStream();
	}

	@Override
	public boolean isHidden() throws IOException {
		return getName().startsWith(".");
	}

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		if(exists()) {
			return false;
		}
		
		try {
			fs.createFolder(fs.getFile(FileUtils.getParentPath(path)), FileUtils.getFilename(path));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isReadable() throws IOException {
		return true;
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {
		String srcPath = src.getAbsolutePath();
		InMemoryFile sourceFileObject = fs.getFile(srcPath);
		if (!sourceFileObject.exists()) {
			throw new IOException(String.format("Source file %s is not present in filesystem create it first.", srcPath));
		}
		
		getFile().copyFrom(sourceFileObject);
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {
		String targetPath = target.getAbsolutePath();
		InMemoryFile targetFileObject = fs.getFile(targetPath);
		if (!targetFileObject.exists()) {
			throw new IOException(String.format("Target file %s is not present in filesystem create it first.", targetPath));
		}
		
		getFile().moveTo(targetFileObject);
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		getFile().delete();
		return true;
	}

	public SftpFileAttributes getAttributes() throws IOException {
		
		if(!exists()) {
			throw new FileNotFoundException();
		}
		
		if(Objects.isNull(this.attrs)) {
			attrs = new SftpFileAttributes(getFileType(), "UTF-8");
			
			attrs.setTimes(new UnsignedInteger64(lastModified() / 1000), 
					new UnsignedInteger64(lastModified() / 1000));
			
			attrs.setPermissions(String.format("%s%s-------", (isReadable() ? "r"
					: "-"), (isWritable() ? "w" : "-")));
			
			
			
			if(!isDirectory()) {
				attrs.setSize(new UnsignedInteger64(length()));
			}
		}
		  
	    return attrs;
	}

	private int getFileType() throws IOException {
		if(isDirectory()) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		} else if(exists()) {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		} else {
			return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
		}
	}

	@Override
	public void refresh() {
	}

	@Override
	public long lastModified() throws IOException {
		return getFile().getLastModified().getTime();
	}

	@Override
	public long length() throws IOException {
		return getFile().getLength();
	}

	@Override
	public boolean isWritable() throws IOException {
		return true;
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		
		if(exists()) {
			return false;
		}
		
		try {
			fs.createFile(fs.getFile(FileUtils.getParentPath(path)), FileUtils.getFilename(path));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		getFile().truncate();
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		this.attrs = attrs;
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return getFile().getPath();
	}

	@Override
	public boolean supportsRandomAccess() {
		return true;
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException {
		return getFile().openFile(writeAccess);
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException {
		if(!exists()) {
			try {
				createNewFile();
			} catch (PermissionDeniedException e) {
				throw new IOException(e);
			}
		}
		return append ? getFile().getAppendOutputStream() : getFile().getOutputStream();
	}

	@Override
	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		if (child == null || child.trim().length() == 0) {
			return this;
		}
		
		String path = null;
		if (child.startsWith("/")) {
			path = child;
		} else {
			child = String.format("/%s", child);
			path = String.format("%s%s", getFile().getPath(), child);
		}
		
		return new InMemoryAbstractFile(path, fs, this.fileFactory);
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return this.fileFactory;
	}
	
	@Override
	public String toString() {
		return path;
	}

}
