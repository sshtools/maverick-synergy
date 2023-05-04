package com.sshtools.agent.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketTransportAdapter implements SshAgentAcceptor {

	private ServerSocket socket;
	
	public ServerSocketTransportAdapter(ServerSocket socket) {
		this.socket = socket;
	}
	
	@Override
	public SshAgentTransport accept() throws IOException {
		Socket sock = socket.accept();
		if(sock==null) {
			return null;
		}
		return new SocketTransportAdapter(sock);
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}
}
