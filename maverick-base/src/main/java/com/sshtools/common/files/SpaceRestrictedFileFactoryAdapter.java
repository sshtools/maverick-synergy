package com.sshtools.common.files;

import java.io.IOException;
import java.io.OutputStream;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.SpaceRestrictedFileFactoryAdapter.SpaceRestrictedAbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusEventException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.util.IOUtils;

public class SpaceRestrictedFileFactoryAdapter implements AbstractFileFactory<SpaceRestrictedAbstractFile> {

	AbstractFileFactory<?> fileFactory;
	long maximumSize;
	
	public SpaceRestrictedFileFactoryAdapter(AbstractFileFactory<?> fileFactory, long maximumSize) {
		this.fileFactory = fileFactory;
		this.maximumSize = maximumSize;
	}
	
	@Override
	public SpaceRestrictedAbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		return new SpaceRestrictedAbstractFile(fileFactory.getFile(path));
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public SpaceRestrictedAbstractFile getDefaultPath() throws PermissionDeniedException, IOException {
		return new SpaceRestrictedAbstractFile(fileFactory.getDefaultPath());
	}
	
	long getCurrentSize(AbstractFile file) throws IOException, PermissionDeniedException {
		
		long size = 0;
		if(file.isDirectory()) {
			for(AbstractFile dir : file.getChildren()) {
				size += getCurrentSize(dir);
			}
		} else {
			return file.length();
		}
		
		return size;
	}
	
	public class SpaceRestrictedAbstractFile extends AbstractFileAdapter {

		public SpaceRestrictedAbstractFile(AbstractFile file) {
			super(file);
		}
		
		public boolean supportsRandomAccess() {
			return false;
		}
		
		@Override
		public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
			return new RestrictedSizeOutputStream(super.getOutputStream(), 
					maximumSize - getCurrentSize(getDefaultPath()));
		}

		@Override
		public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
			return new RestrictedSizeOutputStream(super.getOutputStream(append), 
					maximumSize - getCurrentSize(getDefaultPath()) - (append ? length() : 0));
		}		
	}
	
	
	static class RestrictedSizeOutputStream extends OutputStream {

		long restrictedSize;
		long bytesWritten = 0;
		OutputStream out;
		RestrictedSizeOutputStream(OutputStream out, long restrictedSize) {
			this.out = out;
			this.restrictedSize = restrictedSize;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			
			
			if(restrictedSize > 0) {
				if(bytesWritten + len > restrictedSize) {
					throw new SftpStatusEventException(SftpStatusException.SSH_FX_QUOTA_EXCEEDED,
							"Out of quota disk space [" + IOUtils.toByteSize(restrictedSize) + "]");
				}
			}
			out.write(b, off, len);
			bytesWritten+=len;
		}
		
		@Override
		public void write(int b) throws IOException {
			
			if(restrictedSize > 0) {
				if(bytesWritten + 1 > restrictedSize) {
					throw new SftpStatusEventException(SftpStatusException.SSH_FX_QUOTA_EXCEEDED,
							"Out of quota disk space [" + IOUtils.toByteSize(restrictedSize) + "]");
				}
			}
			
			out.write(b);
			++bytesWritten;
		}
		
		
		public void close() throws IOException {
			out.close();
		}
		
		
		
	}
}
