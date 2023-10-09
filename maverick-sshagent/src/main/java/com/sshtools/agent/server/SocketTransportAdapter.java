package com.sshtools.agent.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketTransportAdapter implements SshAgentTransport {
	
	private Socket sock;
	
	public SocketTransportAdapter(Socket sock) {
		this.sock = sock;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return sock.getInputStream();
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		return sock.getOutputStream();
	}
	
	@Override
	public void close() throws IOException {
		sock.close();
	}
}