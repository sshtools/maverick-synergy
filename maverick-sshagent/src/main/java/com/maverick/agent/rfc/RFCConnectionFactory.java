package com.maverick.agent.rfc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.maverick.agent.KeyStore;
import com.maverick.agent.server.SshAgentConnection;
import com.maverick.agent.server.SshAgentConnectionFactory;

public class RFCConnectionFactory implements SshAgentConnectionFactory {

	@Override
	public SshAgentConnection createConnection(KeyStore keystore, InputStream in, OutputStream out, Closeable closeable) throws IOException {
		return new RFCAgentConnection(keystore, in, out, closeable);
	}

}
