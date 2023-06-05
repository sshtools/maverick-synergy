/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
