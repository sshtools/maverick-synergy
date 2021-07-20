
package com.sshtools.common.tests;

import java.io.Closeable;
import java.io.IOException;

import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;

public interface ForwardingTestTemplate<T extends Closeable> {

	T createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException;
	
	int startForwarding(T client, int targetPort) throws UnauthorizedException, SshException;

	void disconnect(T client);
}
