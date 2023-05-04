package com.sshtools.synergy.ssh;

import com.sshtools.common.ssh.SshException;

public interface RemoteForwardRequestHandler<T extends SshContext> {

	boolean isHandled(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<T> conn);
	
	int startRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<T> conn) throws SshException;
	
	void stopRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<T> conn) throws SshException;
}
