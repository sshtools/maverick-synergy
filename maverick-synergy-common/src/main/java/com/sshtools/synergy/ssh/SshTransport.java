
package com.sshtools.synergy.ssh;

import com.sshtools.common.sshd.SshMessage;

public interface SshTransport<T extends SshContext> {

	void postMessage(SshMessage msg);

	void postMessage(SshMessage msg, boolean kex);

	T getContext();

	void disconnect(int keyExchangeFailed, String string);

	void sendNewKeys();

}
