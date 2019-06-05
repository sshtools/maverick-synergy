package com.sshtools.common.ssh;

import java.io.InputStream;
import java.io.OutputStream;

public interface SessionChannelServer extends SessionChannel {
	
	InputStream getInputStream();
	
	OutputStream getOutputStream();
	
	OutputStream getErrorStream();
}
