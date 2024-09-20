package com.sshtools.common.sftp;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
