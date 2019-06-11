package com.maverick.agent.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SshAgentTransport extends Closeable {

	InputStream getInputStream() throws IOException;
	
	OutputStream getOutputStream() throws IOException;
	
	void close() throws IOException;
}
