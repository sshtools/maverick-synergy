
package com.sshtools.agent.openssh;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.agent.KeyStore;
import com.sshtools.agent.server.SshAgentConnection;
import com.sshtools.agent.server.SshAgentConnectionFactory;

public class OpenSSHConnectionFactory implements SshAgentConnectionFactory {

	@Override
	public SshAgentConnection createConnection(KeyStore keystore, InputStream in, OutputStream out, Closeable closeable) throws IOException {
		return new OpenSSHAgentConnection(keystore, in ,out, closeable);
	}

}
