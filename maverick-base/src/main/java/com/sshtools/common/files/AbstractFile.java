package com.sshtools.common.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;

public interface AbstractFile {

	public abstract String getName();

	public abstract InputStream getInputStream() throws IOException;

	public abstract boolean exists() throws IOException;

	public abstract List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException;

	public abstract String getAbsolutePath() throws IOException, PermissionDeniedException;

	public abstract boolean isDirectory() throws IOException;

	public abstract boolean isFile() throws IOException;

	public abstract OutputStream getOutputStream() throws IOException;

	public abstract boolean isHidden() throws IOException;

	public abstract boolean createFolder() throws PermissionDeniedException, IOException;

	public abstract boolean isReadable() throws IOException;

	public abstract void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException;

	public abstract void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException;

	public abstract boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException;

	public abstract SftpFileAttributes getAttributes() throws FileNotFoundException, IOException, PermissionDeniedException;

	public abstract void refresh();
	
	long lastModified() throws IOException;

	long length() throws IOException;

	boolean isWritable() throws IOException;

	boolean createNewFile() throws PermissionDeniedException, IOException;
	
	void truncate() throws PermissionDeniedException, IOException;

	void setAttributes(SftpFileAttributes attrs) throws IOException;

	String getCanonicalPath() throws IOException, PermissionDeniedException;
	
	boolean supportsRandomAccess();
	
	AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException;

	OutputStream getOutputStream(boolean append) throws IOException;
	
	AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException;
	
	AbstractFileFactory<? extends AbstractFile> getFileFactory();
	
}
