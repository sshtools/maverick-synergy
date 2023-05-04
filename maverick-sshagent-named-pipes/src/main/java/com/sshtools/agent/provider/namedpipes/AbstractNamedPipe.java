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
package com.sshtools.agent.provider.namedpipes;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.common.logger.Log;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public abstract class AbstractNamedPipe implements Closeable {

	protected final int MAX_BUFFER_SIZE = 1024;
	protected String pipeName;
	public static final String NAMED_PIPE_PREFIX = "\\\\.\\pipe\\";

	
	
	AbstractNamedPipe(String pipeName) {
		this.pipeName = pipeName;
		
		if(!pipeName.startsWith(NAMED_PIPE_PREFIX)) {
			pipeName = NAMED_PIPE_PREFIX + pipeName;
		}
		
		this.pipeName = pipeName;
		
		if(Log.isInfoEnabled()) {
			Log.info("Creating pipe {}", this.pipeName);
		}
	}
	
	public String getPath() {
		return pipeName;
	}
	
	public class NamedPipeSession {
		
		HANDLE hNamedPipe;
		InputStream in;
		OutputStream out;
		NamedPipeSession(HANDLE hNamedPipe) {
			this.hNamedPipe = hNamedPipe;
			this.in = new NamedPipeInputStream(this);
			this.out = new NamedPipeOutputStream(this);
		}
		
		public InputStream getInputStream() {
			return in;
		}
		
		public OutputStream getOutputStream() {
			return out;
		}
		
		public void close() throws IOException {
			
			if(Log.isInfoEnabled()) {
				Log.info("Closing pipe {} handle={}", pipeName, hNamedPipe.toString());
			}
			assertCallSucceeded("DisconnectNamedPipe", Kernel32.INSTANCE.DisconnectNamedPipe(hNamedPipe));
			assertCallSucceeded("Named pipe handle close", Kernel32.INSTANCE.CloseHandle(hNamedPipe));
		}
	}	
	
    class NamedPipeInputStream extends InputStream {

    	NamedPipeSession session;
    	
    	NamedPipeInputStream(NamedPipeSession session) {
			this.session = session;
		}
    	
		@Override
		public int read() throws IOException {
			byte[] tmp = new byte[1];
			int r = read(tmp);
			if(r==1) {
				return tmp[0] & 0xFF;
			}
			return -1;
		}
    	
    	public int read(byte[] buf, int off, int len) throws IOException {
    		
	    	try {
	    		byte[] tmp = new byte[len];
	    		
				if(Log.isInfoEnabled()) {
					Log.info("Reading {} maximum bytes from pipe {} handle={}", len, pipeName, session.hNamedPipe.toString());
				}
				
	            IntByReference lpNumberOfBytesRead = new IntByReference(0);
	            assertCallSucceeded("ReadFile", Kernel32.INSTANCE.ReadFile(session.hNamedPipe, tmp, tmp.length, lpNumberOfBytesRead, null));
	
	            int readSize = lpNumberOfBytesRead.getValue();
	            if(readSize > 0) {
	            	System.arraycopy(tmp, 0, buf, off, readSize);
	            }
	            
				if(Log.isInfoEnabled()) {
					Log.info("Read {} bytes from pipe {} handle={}", readSize, pipeName, session.hNamedPipe.toString());
				}
				
	            return readSize;
    		} catch(Throwable t) {
    			if(Log.isErrorEnabled()) {
    				Log.error("Error reading bytes from pipe {} handle={}", t, pipeName, session.hNamedPipe.toString());
    			}
    			close();
    			return -1;
    		}
    	}
    	
    	public void close() throws IOException {
    		session.close();
    	}
    }
    
    class NamedPipeOutputStream extends OutputStream {

    	NamedPipeSession session;
    	
    	NamedPipeOutputStream(NamedPipeSession session) {
			this.session = session;
		}
		@Override
		public void write(int b) throws IOException {
			byte[] buf = new byte[1];
			buf[0] = (byte) b;
			write(buf);
		}
    	
		public void write(byte[] buf, int off, int len) throws IOException {
			
			if(Log.isInfoEnabled()) {
				Log.info("Writing {} bytes to pipe {} handle={}", len, pipeName, session.hNamedPipe.toString());
			}
			
			byte[] tmp = new byte[len];
			System.arraycopy(buf, off, tmp, 0, len);
			
			IntByReference lpNumberOfBytesWritten = new IntByReference(0);
            assertCallSucceeded("WriteFile", Kernel32.INSTANCE.WriteFile(session.hNamedPipe, tmp, len, lpNumberOfBytesWritten, null));
            assertEquals("Mismatched write buffer size", len, lpNumberOfBytesWritten.getValue());

            if(Log.isInfoEnabled()) {
				Log.info("Written {} bytes to pipe {} handle={}", lpNumberOfBytesWritten.getValue(), pipeName, session.hNamedPipe.toString());
			}
		}
		
		public void flush() throws IOException {
//            if(Log.isInfoEnabled()) {
//				Log.info("Flushing pipe {} handle={}", pipeName, session.hNamedPipe.toString());
//			}
//            assertCallSucceeded("FlushFileBuffers", Kernel32.INSTANCE.FlushFileBuffers(session.hNamedPipe));
		}
		
    	public void close() throws IOException {
    		session.close();
    	}
    }
    
	/**
	 * Checks if the API call result is {@code true}. If not, then calls
	 * {@link Kernel32#GetLastError()} and fails with the error code.
	 * <B>Note:</B> if the error code is {@link WinError#ERROR_SUCCESS}
	 * then an <I>&quot;unknown reason code&quot;</I> is reported
	 * @param message Message to display if call failed
	 * @param result The API call result
	 */
	public static final void assertCallSucceeded(String message, boolean result) throws IOException {
	    if (result) {
	        return;
	    }
	
	    int hr = Kernel32.INSTANCE.GetLastError();
	    if( hr == WinError.ERROR_MORE_DATA) {
	     	return;
	    }
	    if (hr == WinError.ERROR_SUCCESS) {
	        throw new IOException(message + " failed with unknown reason code");
	    } else {
	    	throw new IOException(message + " failed: hr=" + hr + " - 0x" + Integer.toHexString(hr));
	    }
	}

	/**
	 * Makes sure that the handle argument is not {@code null} or {@link WinBase#INVALID_HANDLE_VALUE}.
	 * If invalid handle detected, then it invokes {@link Kernel32#GetLastError()}
	 * in order to display the error code
	 * @param message Message to display if bad handle
	 * @param handle The {@link HANDLE} to test
	 * @return The same as the input handle if good handle - otherwise does
	 * not return and throws an assertion error
	 */
	public static final HANDLE assertValidHandle(String message, HANDLE handle) throws IOException {
	    if ((handle == null) || WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
	        int hr = Kernel32.INSTANCE.GetLastError();
	        if (hr == WinError.ERROR_SUCCESS) {
	            throw new IOException(message + " failed with unknown reason code");
	        } else {
	            throw new IOException(message + " failed: hr=" + hr + " - 0x" + Integer.toHexString(hr));
	        }
	    }
	
	    return handle;
	}

	protected void assertEquals(String message, int len, int value) throws IOException {
		if(len!=value) {
			throw new IOException(message);
		}
	}

	public AbstractNamedPipe() {
		super();
	}

}