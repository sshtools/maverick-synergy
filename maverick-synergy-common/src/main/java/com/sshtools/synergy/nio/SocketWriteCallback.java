package com.sshtools.synergy.nio;

/**
 * Provides a callback when the socket has completed a socket write operation.
 */
public interface SocketWriteCallback {
	void completedWrite();
}
