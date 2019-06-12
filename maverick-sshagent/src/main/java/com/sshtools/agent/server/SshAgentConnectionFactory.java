package com.maverick.agent.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.maverick.agent.KeyStore;

public interface SshAgentConnectionFactory {

	SshAgentConnection createConnection(KeyStore keystore, InputStream in, OutputStream out, Closeable closeable) throws IOException;
}
