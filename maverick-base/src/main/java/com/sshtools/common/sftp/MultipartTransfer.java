package com.sshtools.common.sftp;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.permissions.PermissionDeniedException;

public interface MultipartTransfer {

	String getPath();
	
	String getUploadId();
	
	OpenFile openPart(Multipart part) throws IOException, PermissionDeniedException;

	void combineParts() throws IOException, PermissionDeniedException;

	Multipart getPart(String partIdentifier);

	Collection<Multipart> getParts();
	
	String getUuid();

	boolean getExists();
	
	void cancel();
	
	boolean isCancelled();
}
