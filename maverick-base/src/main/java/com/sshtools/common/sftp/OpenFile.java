package com.sshtools.common.sftp;

import java.io.IOException;
import java.util.Optional;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.UnsignedInteger32;

public interface OpenFile {

	AbstractFile getFile();

	default Optional<UnsignedInteger32> getAccessFlags() {
		return Optional.empty();
	}

	UnsignedInteger32 getFlags();

	boolean isTextMode();

	long getFilePointer() throws IOException;

	void seek(long longValue) throws IOException;

	int read(byte[] buf, int start, int numBytesToRead) throws IOException, PermissionDeniedException;

	void write(byte[] data, int off, int len) throws IOException, PermissionDeniedException;

	void close() throws IOException;

	void processEvent(Event evt);

	byte[] getHandle();
	
	default void lock(long offset, long length, int lockFlags) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	default void unlock(long offset, long length) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	default int lockFlags() {
		throw new UnsupportedOperationException();
	}
	
	default boolean isLocked() {
		return lockFlags() > -1;
	}

}
