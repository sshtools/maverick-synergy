package com.sshtools.common.ssh;

public class ConnectionStateAdapter<T extends SshContext> implements ConnectionStateListener<T> {

	@Override
	public void connected(Connection<T> con) {
		
	}

	@Override
	public void disconnected(Connection<T> con) {

	}

}
