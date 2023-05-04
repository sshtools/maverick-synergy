package com.sshtools.common.ssh;

import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.common.util.UnsignedInteger32;

public interface SessionChannel extends Channel {

	SshConnection getConnection();

	void haltIncomingData();

	void resumeIncomingData();
	
	UnsignedInteger32 getMaximumWindowSpace();

	UnsignedInteger32 getMinimumWindowSpace();

	/**
	 * Called once the session is open and data can be sent/received. This event
	 * happens once the user has either started the shell or executed a command.
	 */
	void onSessionOpen();
	
	InputStream getInputStream();
	
	OutputStream getOutputStream();
	
}
