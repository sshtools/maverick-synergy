
package com.sshtools.common.sshd;

import com.sshtools.common.nio.IdleStateListener;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;

public interface AbstractServerTransport<C extends Context> {

	void disconnect(int reason, String message);

	C getContext();

	void postMessage(SshMessage sshMessage, boolean kex);

	void sendNewKeys();
	
	SshConnection getConnection();

	void postMessage(SshMessage sshMessage);

	boolean isConnected();

	void addTask(Integer messageQueue, ConnectionAwareTask r);

	void startService(Service<C> service);

	byte[] getSessionKey();

	void registerIdleStateListener(IdleStateListener listener);

	void removeIdleStateListener(IdleStateListener listener);

	void resetIdleState(IdleStateListener listener);

	boolean isSelectorThread();

}
