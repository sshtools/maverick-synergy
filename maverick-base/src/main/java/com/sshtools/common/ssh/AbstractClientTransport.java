package com.sshtools.common.ssh;

import com.sshtools.common.sshd.SshMessage;

public interface AbstractClientTransport<C extends Context> {

	C getContext();

	void disconnect(int reason, String message);

	void postMessage(SshMessage msg, boolean kex);

	void sendNewKeys();
}
