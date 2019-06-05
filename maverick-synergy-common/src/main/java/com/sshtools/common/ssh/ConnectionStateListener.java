package com.sshtools.common.ssh;

public interface ConnectionStateListener<T extends SshContext> {

	public void connected(Connection<T> con);
	
	public void disconnected(Connection<T> con);

}
