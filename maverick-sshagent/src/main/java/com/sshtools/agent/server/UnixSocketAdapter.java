
package com.sshtools.agent.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class UnixSocketAdapter implements SshAgentAcceptor {

	ServerSocket socket;
	UnixSocketAdapter(ServerSocket socket) {
		this.socket = socket;
	}
	@Override
	public SshAgentTransport accept() throws IOException {
		Socket sock = socket.accept();
		if(sock==null) {
			return null;
		}
		return new UnixSocketTransport(sock);
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	class UnixSocketTransport implements SshAgentTransport {
		
		Socket sock;
		UnixSocketTransport(Socket sock) {
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
}
