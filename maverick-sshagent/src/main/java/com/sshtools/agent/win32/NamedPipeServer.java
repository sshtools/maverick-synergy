/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.agent.win32;

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
