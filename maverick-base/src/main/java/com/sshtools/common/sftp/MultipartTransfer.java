package com.sshtools.common.sftp;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;

public interface MultipartTransfer {
	
	int getMinimumPartSize();
	
	String getPath();
	
	AbstractFile getFile();
	
	String getUploadId();
	
	OpenFile openPart(Multipart part) throws IOException, PermissionDeniedException;

	void combineParts() throws IOException, PermissionDeniedException;

	Multipart getPart(String partIdentifier);

	Collection<Multipart> getParts();
	
	String getUuid();

	boolean getExists();
	
	void cancel();
	
	boolean isCancelled();
	
	MultipartTransfer onComplete(MultipartCompletionCallback transfer);
	
	interface MultipartCompletionCallback {
		
		void multipartCompleted(MultipartTransfer transfer);
	}
}
