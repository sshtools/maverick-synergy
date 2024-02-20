package com.sshtools.common.files;

import java.io.IOException;
import java.io.OutputStream;

import com.sshtools.common.permissions.PermissionDeniedException;

public abstract class AbstractFileImpl<T extends AbstractFile> implements AbstractFile {

	protected AbstractFileFactory<T> fileFactory;

	public AbstractFileImpl(AbstractFileFactory<T> fileFactory) {
		this.fileFactory = fileFactory;
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}
	
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		if(!append) {
			return getOutputStream();
		}
		
		return new AppendOutputStream();
	}

    class AppendOutputStream extends OutputStream {

    	AbstractFileRandomAccess content;
    	
    	AppendOutputStream() throws IOException, PermissionDeniedException {
    		if(!exists()) {
    			try {
					createNewFile();
				} catch (PermissionDeniedException e) {
					throw new IOException(e.getMessage(), e);
				}
    		}
    		content = openFile(true);
    		try {
				content.seek(getAttributes().size().longValue());
			} catch (PermissionDeniedException e) {
				throw new IOException(e.getMessage(), e);
			}
    	}
		@Override
		public void write(int b) throws IOException {
			content.write(new byte[] { (byte)b },0,1);
		}
		
		public void write(byte[] buf, int off, int len) throws IOException {
			content.write(buf, off, len);
		}
		
		public void close() throws IOException {
			content.close();
		}
    	
    }
    
    public AbstractFileFactory<T> getFileFactory() {
    	return fileFactory;
    }

    protected abstract int doHashCode();
    
	@Override
	public final int hashCode() {
		return doHashCode();
	}
	
	protected abstract boolean doEquals(Object obj);

	@Override
	public final boolean equals(Object obj) {
		return doEquals(obj);
	}
    
    
}
