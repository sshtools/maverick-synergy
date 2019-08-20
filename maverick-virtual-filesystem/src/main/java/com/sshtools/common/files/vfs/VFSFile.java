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
package com.sshtools.common.files.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.util.RandomAccessMode;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileImpl;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger64;

public class VFSFile extends AbstractFileImpl<VFSFile> {

	

	FileObject file;
	FileSystemOptions opts;

	public VFSFile(FileObject file, VFSFileFactory fileFactory,
			SshConnection con) {
		super(fileFactory, con);
		this.file = file;
	}

	public FileObject getFileObject() {
		return file;
	}

	public VFSFile(String path, VFSFileFactory fileFactory,
			SshConnection con) throws IOException {
		super(fileFactory, con);
		this.file = fileFactory.getFileSystemManager().resolveFile(path);
	}

	public VFSFile(String path, String defaultPath, VFSFileFactory fileFactory,
			SshConnection con, FileSystemOptions opts)
			throws IOException {
		super(fileFactory, con);
		this.file = fileFactory.getFileSystemManager().resolveFile(path, opts);
		this.opts = opts;
	}

	public boolean exists() throws IOException {
		return file.exists();
	}

	public boolean createFolder() throws PermissionDeniedException, IOException {
		if(!file.exists()) {
			file.createFolder();
			return file.exists();
		}
		return false;
	}

	public long lastModified() throws IOException {
		return file.getContent().getLastModifiedTime();
	}

	public String getName() {
		return file.getName().getBaseName();
	}

	public long length() throws IOException {
		if (file.getType() == FileType.FILE) {
			return file.getContent().getSize();
		} else {
			return 0;
		}
	}

	public SftpFileAttributes getAttributes() throws IOException {

		if(!exists()) {
			throw new FileNotFoundException();
		}
		SftpFileAttributes attr = new SftpFileAttributes(getFileType(file), "UTF-8");
		
		if (attr.isDirectory())
			attr.setSize(new UnsignedInteger64(length()));
		try {
			attr.setTimes(new UnsignedInteger64(lastModified() / 1000),
					new UnsignedInteger64(lastModified() / 1000));
		} catch (FileSystemException e) {
		}
		
		attr.setPermissions(String.format("%s%s-------", (isReadable() ? "r"
				: "-"), (isWritable() ? "w" : "-")));


		try {
			for (String name : file.getContent().getAttributeNames()) {
				Object attribute = file.getContent().getAttribute(name);
				attr.setExtendedAttribute(name,
						attribute == null ? new byte[] {} : String.valueOf(attribute).getBytes());

				if (name.equals("uid")) {
					attr.setUID((String) attribute);
				} else if (name.equals("gid")) {
					attr.setGID((String) attribute);
				} else if (name.equals("accessedTime")) {
					attr.setTimes(new UnsignedInteger64((Long) attribute),
							attr.getModifiedTime());
				} 
			}
		} catch (Exception e) {

		}

		return attr;
	}

	private int getFileType(FileObject file) throws FileSystemException {
		
		try {
			for (String name : file.getContent().getAttributeNames()) {
				Object attribute = file.getContent().getAttribute(name);

				if (name.equals("link")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK;
				} else if (name.equals("block")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_BLOCK_DEVICE;
				} else if(name.equals("character")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_CHAR_DEVICE;
				} else if(name.equals("socket")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_SOCKET;
				} else if(name.equals("fifo")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_FIFO;
				} else if(name.equals("pipe")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_SPECIAL;
				}
			}
		} catch (Exception e) {
		}
		
		switch (file.getType()) {
		case FILE:
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		case FOLDER:
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		default:
			return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
		}
	}

	public boolean isHidden() throws IOException {
		return file.isHidden();
	}

	public boolean isDirectory() throws IOException {
		return file.getType() == FileType.FOLDER;
	}

	public List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException {

		List<AbstractFile> children = new ArrayList<AbstractFile>();
		for (FileObject f : file.getChildren()) {
			children.add(new VFSFile(f, (VFSFileFactory) fileFactory, con));
		}
		return children;
	}

	public boolean isFile() throws IOException {
		return file.getType() == FileType.FILE;
	}

	public String getAbsolutePath() throws IOException,
			PermissionDeniedException {
		if (!((VFSFileFactory) getFileFactory()).isReturnURIForPath()) {
			return file.getName().getPath();
		} else {
			return file.getName().getURI();
		}
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException {
		if (src instanceof VFSFile) {
			file.copyFrom(((VFSFile) src).file, new AllFileSelector());
		} else {
			super.copyFrom(src);
		}
	}

	public boolean isReadable() throws IOException {
		return file.isReadable();
	}

	public boolean isWritable() throws IOException {
		return file.isWriteable();
	}

	public boolean createNewFile() throws PermissionDeniedException,
			IOException {
		file.createFile();
		return true;
	}

	public void truncate() throws PermissionDeniedException, IOException {
		OutputStream out = file.getContent().getOutputStream();
		out.close();
	}

	public InputStream getInputStream() throws IOException {
		return file.getContent().getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return file.getContent().getOutputStream();
	}

	public boolean delete(boolean recurse) throws IOException {
		if (recurse) {
			file.delete(new AllFileSelector());
			return true;
		}
		return file.delete();
	}

	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {
		if (target instanceof VFSFile) {
			file.moveTo(((VFSFile) target).file);
		} else {
			super.moveTo(target);
		}

	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		
		file.getContent().setLastModifiedTime(
					attrs.getModifiedTime().longValue() * 1000);

		// For SSH it's easy
		if (file.getFileSystem().getRootName().getScheme().equals("sftp")) {
			if (attrs.getUID() != null) {
				file.getContent().setAttribute("uid", attrs.getUID());
			}
			if (attrs.getGID() != null) {
				file.getContent().setAttribute("gid", attrs.getGID());
			}
			if (attrs.getPermissions() != null) {
				file.getContent().setAttribute("permissions",
						attrs.getPermissions().intValue());
			}
		}

	}

	public String getCanonicalPath() throws IOException,
			PermissionDeniedException {
		return file.getName().getURI();
	}

	public boolean supportsRandomAccess() {
		return file.getFileSystem()
				.hasCapability(Capability.RANDOM_ACCESS_READ)
				|| file.getFileSystem().hasCapability(
						Capability.RANDOM_ACCESS_WRITE);
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess)
			throws IOException {
		return new VFSFileRandomAccess(file.getContent()
				.getRandomAccessContent(
						writeAccess ? RandomAccessMode.READWRITE
								: RandomAccessMode.READ));
	}

	class VFSFileRandomAccess implements AbstractFileRandomAccess {

		RandomAccessContent randomAccessContent;

		public VFSFileRandomAccess(RandomAccessContent randomAccessContent) {
			this.randomAccessContent = randomAccessContent;
		}

		public int read(byte[] buf, int off, int len) throws IOException {

			long length = Math.min(randomAccessContent.length()
					- randomAccessContent.getFilePointer(), len);

			if (length <= 0) {
				return -1;
			}

			randomAccessContent.readFully(buf, off, (int) length);
			return (int) length;
		}

		public void write(byte[] buf, int off, int len) throws IOException {
			randomAccessContent.write(buf, off, len);
		}

		public void setLength(long length) throws IOException {
			long pos = randomAccessContent.getFilePointer();
			if (length > pos) {
				randomAccessContent.seek(pos - 1);
				randomAccessContent.write(0);
				randomAccessContent.seek(pos);
			}
		}

		public void seek(long position) throws IOException {
			randomAccessContent.seek(position);
		}

		public void close() throws IOException {
			randomAccessContent.close();

		}

		public long getFilePointer() throws IOException {
			return randomAccessContent.getFilePointer();
		}

	}

	public void refresh() {
		try {
			file.refresh();
		} catch (FileSystemException e) {
			Log.error("Failed to refresh.", e);
		}
	}

	public AbstractFile resolveFile(String child) throws IOException,
			PermissionDeniedException {
		return new VFSFile(file.resolveFile(child),
				(VFSFileFactory) fileFactory, con);
	}

}
