package com.sshtools.common.ssh;

import java.io.OutputStream;

public interface SessionChannelServer extends SessionChannel {

	OutputStream getErrorStream();
	
	void enableRawMode();

	void disableRawMode();

	boolean setEnvironmentVariable(String name, String value);
}
