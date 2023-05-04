package com.sshtools.common.sftp;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.UnsignedInteger32;

public interface OpenFile {

	AbstractFile getFile();

	UnsignedInteger32 getFlags();

	boolean isTextMode();

	long getFilePointer() throws IOException;

	void seek(long longValue) throws IOException;

	int read(byte[] buf, int start, int numBytesToRead) throws IOException, PermissionDeniedException;

	void write(byte[] data, int off, int len) throws IOException, PermissionDeniedException;

	void close() throws IOException;

	void processEvent(Event evt);

	byte[] getHandle();

}
