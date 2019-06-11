package com.maverick.agent.win32;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class NamedPipeClient extends AbstractNamedPipe {

	NamedPipeSession session;
	
	public NamedPipeClient(String pipeName) throws IOException {
		super(pipeName);
		
		HANDLE hPipe = assertValidHandle("CreateFile", Kernel32.INSTANCE.CreateFile(getPath(),
                WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                0,
                null,
                WinNT.OPEN_EXISTING,
                0,
                null));
		
		this.session = new NamedPipeSession(hPipe);
	}
	
	public InputStream getInputStream() {
		return session.getInputStream();
	}
	
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}
	
	public void close() throws IOException {
		session.close();
	}
}
