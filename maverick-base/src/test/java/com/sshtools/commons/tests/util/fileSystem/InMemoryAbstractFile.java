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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.commons.tests.util.fileSystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.ssh.SshConnection;

public class InMemoryAbstractFile implements AbstractFile {

    private final InMemoryFile file;
    private SftpFileAttributes attrs;
    private AbstractFileFactory<InMemoryAbstractFile> fileFactory;
    private SshConnection sshConnection;
    
    
    public InMemoryAbstractFile(InMemoryFile file, AbstractFileFactory<InMemoryAbstractFile> fileFactory, SshConnection sshConnection) {
    	this.file = file;
    	this.fileFactory = fileFactory;
    	this.sshConnection = sshConnection;
	}
	
	@Override
	public String getName() {
		return this.file.getName();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.file.getInputStream();
	}

	@Override
	public boolean exists() throws IOException {
		return this.file.exists();
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return this.file.getChildren().stream().map((fo) -> {
			return new InMemoryAbstractFile(fo, InMemoryAbstractFile.this.fileFactory, InMemoryAbstractFile.this.sshConnection);
		}).collect(Collectors.toList());
	}

	@Override
	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return this.file.getPath();
	}

	@Override
	public boolean isDirectory() throws IOException {
		return this.file.isFolder();
	}

	@Override
	public boolean isFile() throws IOException {
		return this.file.isFile();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return this.file.getOutputStream();
	}

	@Override
	public boolean isHidden() throws IOException {
		return getName().startsWith(".");
	}

	@Override
	public boolean createFolder() throws PermissionDeniedException, IOException {
		// FileObject is all ready in created
		return true;
	}

	@Override
	public boolean isReadable() throws IOException {
		return true;
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException, PermissionDeniedException {
		String srcPath = src.getAbsolutePath();
		InMemoryFile sourceFileObject = this.file.getfileSystem().getFile(srcPath);
		if (!sourceFileObject.exists()) {
			throw new IOException(String.format("Source file %s is not present in filesystem create it first.", srcPath));
		}
		
		this.file.copyFrom(sourceFileObject);
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException, PermissionDeniedException {
		String targetPath = target.getAbsolutePath();
		InMemoryFile targetFileObject = this.file.getfileSystem().getFile(targetPath);
		if (!targetFileObject.exists()) {
			throw new IOException(String.format("Target file %s is not present in filesystem create it first.", targetPath));
		}
		
		this.file.moveTo(targetFileObject);
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		this.file.delete();
		return true;
	}

	@Override
	public SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException {
		return this.attrs;
	}

	@Override
	public void refresh() {
	}

	@Override
	public long lastModified() throws IOException {
		return this.file.getLastModified().getTime();
	}

	@Override
	public long length() throws IOException {
		return this.file.getLength();
	}

	@Override
	public boolean isWritable() throws IOException {
		return true;
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		// FileObject is already in created state
		return true;
	}

	@Override
	public void truncate() throws PermissionDeniedException, IOException {
		this.file.truncate();
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		this.attrs = attrs;
	}

	@Override
	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return this.file.getPath();
	}

	@Override
	public boolean supportsRandomAccess() {
		return true;
	}

	@Override
	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException {
		return this.file.openFile(writeAccess);
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException {
		return append ? this.file.getAppendOutputStream() : this.file.getOutputStream();
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
			path = String.format("%s%s", this.file.getPath(), child);
		}
		
		InMemoryFile fileObject = null;
		
		if (this.file.getfileSystem().exists(path)) { 
			fileObject = this.file.getfileSystem().getFile(path);
		} else {
			fileObject = this.file.getfileSystem().createFileWithParents(path);
		}
		
		return new InMemoryAbstractFile(fileObject, this.fileFactory, this.sshConnection);
	}

	@Override
	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return this.fileFactory;
	}
	
	@Override
	public String toString() {
		return this.file.getPath();
	}

}
