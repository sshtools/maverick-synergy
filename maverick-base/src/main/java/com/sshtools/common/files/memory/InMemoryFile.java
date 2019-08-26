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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sshtools.common.files.AbstractFileRandomAccess;

public class InMemoryFile {
	
	public static final int DEFAULT_FILE_SIZE = 1024;
	
	private InMemoryFile parent;
	private final InMemoryFileSystem fileSystem;
	private String name;
	private final String id = UUID.randomUUID().toString();
	private boolean folder = false;
	private final Date created = new Date();
	private Date lastModified = new Date();
	private byte[] data;
	private List<InMemoryFile> children = new ArrayList<>();
	
	public InMemoryFile(InMemoryFile parent, InMemoryFileSystem fileSystem, String name, boolean folder) {
		this.parent = parent;
		this.fileSystem = fileSystem;
		this.name = name;
		this.folder = folder;
		
		if(parent!=null) {
			this.parent.children.add(this);
		}
		if (this.folder) {
			this.data = new byte[0];
		} else {
			this.data = new byte[DEFAULT_FILE_SIZE];
		}
	}

	public InMemoryFile createFolder(String name) throws IOException {
		return this.fileSystem.createFolder(this, name);
	}
	
	public InMemoryFile createFile(String name) throws IOException {
		return this.fileSystem.createFile(this, name);
	}
	
	public String getPath() {
		if ("root".equals(this.name)) {
			return InMemoryFileSystem.PATH_SEPARATOR;
		}
		StringBuilder path = new StringBuilder();
		computePath(path, this);
		return path.toString();
	}
	
	public boolean isFolder() {
		return this.folder;
	}
	
	public boolean isFile() {
		return !this.isFolder();
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getId() {
		return this.id;
	}
	
	public Date getCreationDate() {
		return this.created;
	}
	
	public Date getLastModified() {
		return this.lastModified;
	}
	
	public InMemoryFile getParent() {
		return this.parent;
	}
	
	public boolean exists() throws IOException {
		return this.fileSystem.exists(this.getPath());
	}
	
	public void delete() throws IOException {
		this.fileSystem.delete(this);
		parent.children.remove(this);
	}
	
	public List<InMemoryFile> getChildren() throws IOException {
		return children;
	}
	
	public int getLength() {
		return this.data.length;
	}
	
	public void truncate() {
		this.data = new byte[0];
	}
	
	public void moveTo(InMemoryFile fileObjectDest) throws IOException {
		this.fileSystem.moveTo(fileObjectDest, this);
	}
	
	public void copyFrom(InMemoryFile fileObjectSrc) throws IOException {
		this.fileSystem.copyFrom(fileObjectSrc, this);
	}
	
	public InputStream getInputStream() {
		return new ByteArrayInputStream(this.data) {
			{
				InMemoryFile.this.fileSystem.acquireLock(InMemoryFile.this.getPath());
			}
			
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					InMemoryFile.this.fileSystem.releaseLock(InMemoryFile.this.getPath());
				}
			}
		};
	}
	
	public OutputStream getOutputStream() {
		return new ByteArrayOutputStream() {
			
			{
				InMemoryFile.this.fileSystem.acquireLock(InMemoryFile.this.getPath());
			}
			
			@Override
			public synchronized void write(int data) {
				super.write(data);
			}
			
			@Override
			public synchronized void write(byte[] data, int off, int len) {
				super.write(data, off, len);
			}
			
			@Override
			public void write(byte[] data) throws IOException {
				super.write(data);
			}
			
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					InMemoryFile.this.data = this.toByteArray();
					InMemoryFile.this.lastModified = new Date();
					InMemoryFile.this.fileSystem.releaseLock(InMemoryFile.this.getPath());
				}
			}
		};
	}
	
	public OutputStream getAppendOutputStream() {
		return new ByteArrayOutputStream() {
			
			{
				InMemoryFile.this.fileSystem.acquireLock(InMemoryFile.this.getPath());
			}
			
			@Override
			public synchronized void write(int data) {
				super.write(data);
				appendData();
			}

			@Override
			public synchronized void write(byte[] data, int off, int len) {
				super.write(data, off, len);
				appendData();
			}
			
			@Override
			public void write(byte[] data) throws IOException {
				super.write(data);
			}
			
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					InMemoryFile.this.fileSystem.releaseLock(InMemoryFile.this.getPath());
				}
			}
			
			private void appendData() {
				int appendAt = InMemoryFile.this.data.length;
				InMemoryFile.this.data = Arrays.copyOf(InMemoryFile.this.data, InMemoryFile.this.data.length + this.size());
				byte[] finalData = Arrays.copyOf(this.buf, this.size());
				System.arraycopy(finalData, 0, InMemoryFile.this.data,  appendAt, this.size());
				InMemoryFile.this.lastModified = new Date();
			}
		};
	}
	
	public AbstractFileRandomAccess openFile(final boolean writeAccess) throws IOException {
		return new AbstractFileRandomAccess() {
			
			{
				InMemoryFile.this.fileSystem.acquireLock(InMemoryFile.this.getPath());
			}
			
			int pointer = 0;
			
			@Override
			public void write(byte[] buf, int off, int len) throws IOException {
				
				if (!writeAccess) {
					throw new IOException("File is not opened in write mode.");
				}
				
				if (this.pointer == InMemoryFile.this.data.length) {
					InMemoryFile.this.data = Arrays.copyOf(InMemoryFile.this.data, InMemoryFile.this.data.length + len);
				} else if ((this.pointer + len) > InMemoryFile.this.data.length) { 
					InMemoryFile.this.data = Arrays.copyOf(InMemoryFile.this.data, this.pointer + len);
				} else if ((this.pointer + len) < InMemoryFile.this.data.length) {
					// no op just documentation
				}
				
				System.arraycopy(buf, off, InMemoryFile.this.data, this.pointer, len);
				pointer+= len;
			}
			
			@Override
			public void setLength(long length) throws IOException {
				InMemoryFile.this.data = Arrays.copyOf(InMemoryFile.this.data, (int) length);
			}
			
			@Override
			public void seek(long position) throws IOException {
				this.pointer = (int) position;
			}
			
			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				if (this.pointer > InMemoryFile.this.data.length) {
					return -1;
				}
				
				int range = len;
				
				if (this.pointer + len > InMemoryFile.this.data.length) {
					range = InMemoryFile.this.data.length - this.pointer;
				}
				
				System.arraycopy(InMemoryFile.this.data, this.pointer, buf, off, range);
				pointer+= range;
				
				int dataPending = InMemoryFile.this.data.length - pointer;
				return  dataPending <= 0 ? -1 : dataPending;
			}
			
			@Override
			public long getFilePointer() throws IOException {
				return this.pointer;
			}
			
			@Override
			public void close() throws IOException {
				InMemoryFile.this.fileSystem.releaseLock(InMemoryFile.this.getPath());
			}
		};
	}
	
	byte[] getData() {
		return this.data;
	}
	
	void setData(byte[] data) {
		this.data = data;
	}
	
	void rename(String name) {
		this.name = name;
		lastModified = new Date();
	}
	
	void setParent(InMemoryFile parent) {
		this.parent = parent;
	}
	
	public static String computePathFrom(InMemoryFile parent, String name) {
		if ("root".equals(parent.name)) {
			return String.format("%s%s", InMemoryFileSystem.PATH_SEPARATOR, name);
		} 
		return String.format("%s%s%s", parent.getPath(), InMemoryFileSystem.PATH_SEPARATOR, name);
	}
	
	public static String getPathSeperator() {
		return InMemoryFileSystem.PATH_SEPARATOR;
	}
	
	private void computePath(StringBuilder path, InMemoryFile fileObject) {
		if (fileObject.parent != null) {
			computePath(path, fileObject.parent);
		}
		
		if ("root".equals(fileObject.name)) {
			return;
		}
		
		path.append(InMemoryFileSystem.PATH_SEPARATOR).append(fileObject.name);
	}
	
	@Override
	public String toString() {
		return this.getPath();
	}
}
