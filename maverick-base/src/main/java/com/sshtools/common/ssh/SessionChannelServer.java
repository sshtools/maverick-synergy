package com.sshtools.common.ssh;

import java.io.OutputStream;

public interface SessionChannelServer extends SessionChannel {

	OutputStream getErrorStream();
	
	default void pauseDataCaching() { };

	default void resumeDataCaching() { };

	boolean setEnvironmentVariable(String name, String value);
}
