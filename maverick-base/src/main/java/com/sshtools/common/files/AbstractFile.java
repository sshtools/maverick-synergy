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
