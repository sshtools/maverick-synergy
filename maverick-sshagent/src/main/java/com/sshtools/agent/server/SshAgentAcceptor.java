package com.maverick.agent.server;

import java.io.Closeable;
import java.io.IOException;

public interface SshAgentAcceptor extends Closeable {

	SshAgentTransport accept() throws IOException;
	
	void close() throws IOException;
}
