package com.sshtools.common.sftp.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.OpenFile;
import com.sshtools.common.util.UnsignedInteger32;

public class RandomAccessOpenFile implements OpenFile { 
		
		AbstractFile f;
		UnsignedInteger32 flags;
		boolean textMode = false;
		AbstractFileRandomAccess raf;
		boolean closed;
		byte[] handle;
		
		public RandomAccessOpenFile(AbstractFile f, UnsignedInteger32 flags, byte[] handle) throws IOException, PermissionDeniedException {
			this.f = f;
			this.flags = flags;
			this.handle = handle;
			raf = f.openFile(((flags.intValue() & AbstractFileSystem.OPEN_WRITE) != 0));
			this.textMode = (flags.intValue() & AbstractFileSystem.OPEN_TEXT) != 0;
			if (isTextMode() && Log.isDebugEnabled()) {
				Log.debug(f.getName() + " is being opened in TEXT mode");
			}
		}

		public boolean isTextMode() {
			return textMode;
		}

		public void close() throws IOException {
			try {
				raf.close();
			} finally {
				raf = null;
			}
			closed = true;
		}

		public int read(byte[] buf, int off, int len) throws IOException, PermissionDeniedException {
			if(closed) {
				return -1;
			}
			return raf.read(buf, off, len);
		}

		public void write(byte[] buf, int off, int len) throws IOException, PermissionDeniedException {
			if(closed) {
				throw new IOException("File has been closed.");
			}
			raf.write(buf, off, len);
		}

		public void seek(long longValue) throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getOutputStream].");
			}
			raf.seek(longValue);
		}

		public AbstractFile getFile() {
			return f;
		}

		public UnsignedInteger32 getFlags() {
			return flags;
		}

		public long getFilePointer() throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getFilePointer].");
			}
			return raf.getFilePointer();
		}

		@Override
		public void processEvent(Event evt) {
			evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE, f);
			evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE_RANDOM_ACCESS, raf);
		}

		@Override
		public byte[] getHandle() {
			return handle;
		}
	}