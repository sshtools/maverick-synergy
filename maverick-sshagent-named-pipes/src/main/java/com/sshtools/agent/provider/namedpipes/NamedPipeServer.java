package com.sshtools.agent.provider.namedpipes;

import java.io.IOException;

import com.sshtools.common.logger.Log;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * @author lgoldstein
 */
public class NamedPipeServer extends AbstractNamedPipe {
    
	HANDLE hNextHandle = null;
	boolean closed;
	
	public NamedPipeServer(String pipeName) throws IOException {
        super(pipeName);
    }
	
	public synchronized void close() {
		
		closed = true;
		if(hNextHandle!=null) {
			if(Log.isInfoEnabled()) {
				Log.info("Closing pipe server {} handle={}", pipeName, hNextHandle.toString());
			}
			try {
				assertCallSucceeded("DisconnectNamedPipe", Kernel32.INSTANCE.DisconnectNamedPipe(hNextHandle));
				assertCallSucceeded("Named pipe handle close", Kernel32.INSTANCE.CloseHandle(hNextHandle));
			} catch (IOException e) {
			}
		}
	}
	
	public NamedPipeSession accept() throws IOException {

		if(closed) {
			throw new IOException("Named pipe has been closed!");
		}
		
    	if(Log.isInfoEnabled()) {
			Log.info("Waiting for client on pipe {}", this.pipeName);
		}
    	
    	try {
    	
    		synchronized(this) {
		    	hNextHandle = assertValidHandle("CreateNamedPipe", Kernel32.INSTANCE.CreateNamedPipe(pipeName,
		                WinBase.PIPE_ACCESS_DUPLEX,        // dwOpenMode
		                WinBase.PIPE_TYPE_MESSAGE | WinBase.PIPE_READMODE_MESSAGE | WinBase.PIPE_WAIT,    // dwPipeMode
		                WinBase.PIPE_UNLIMITED_INSTANCES,    // nMaxInstances,
		                MAX_BUFFER_SIZE,    // nOutBufferSize,
		                MAX_BUFFER_SIZE,    // nInBufferSize,
		                0,    // nDefaultTimeOut,
		                null    // lpSecurityAttributes
		              ));
    		}
    	
    		if(Log.isInfoEnabled()) {
    			Log.info("Waiting for client to connect to pipe {} handle={}", this.pipeName, hNextHandle.toString());
    		}
    		
	        assertCallSucceeded("ConnectNamedPipe", Kernel32.INSTANCE.ConnectNamedPipe(hNextHandle, null));
	       
	        if(Log.isInfoEnabled()) {
				Log.info("Client connected to pipe {} handle={}", this.pipeName, hNextHandle.toString());
			}
	        
	        synchronized(this) {
	        	try {
	        		return new NamedPipeSession(hNextHandle);
	        	} finally {
	        		hNextHandle = null;
	        	}
	        }
        
    	} catch(Throwable t) {
    		if(Log.isErrorEnabled()) {
    			Log.error("Accept failure", t);
    		}
    		throw new IOException(t.getMessage(), t);
    	}
    }
    
	
}
