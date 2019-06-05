package com.sshtools.common.ssh;

public interface SessionChannel extends Channel {

	SshConnection getConnection();

	void haltIncomingData();

	void resumeIncomingData();
	
	int getMaximumWindowSpace();

	int getMinimumWindowSpace();

	/**
	 * Called once the session is open and data can be sent/received. This event
	 * happens once the user has either started the shell or executed a command.
	 */
	void onSessionOpen();

	boolean isEOF();

	
}
