package com.sshtools.agent.provider.namedpipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.agent.server.SshAgentAcceptor;
import com.sshtools.agent.server.SshAgentTransport;

public class NamedPipeAcceptor implements SshAgentAcceptor {

	NamedPipeServer namedPipe;
	
	public NamedPipeAcceptor(NamedPipeServer namedPipe) {
		this.namedPipe = namedPipe;
	}
	
	@Override
	public SshAgentTransport accept() throws IOException {
		NamedPipeServer.NamedPipeSession session = namedPipe.accept();
		if(session==null) {
			return null;
		}
		return new NamedPipeTransport(session);
	}

	@Override
	public void close() throws IOException {
		namedPipe.close();
	}

	class NamedPipeTransport implements SshAgentTransport {

		NamedPipeServer.NamedPipeSession session;
		
		NamedPipeTransport(NamedPipeServer.NamedPipeSession session) {
			this.session = session;
		}
		
		@Override
		public void close() throws IOException {
			session.close();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return session.getInputStream();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return session.getOutputStream();
		}
		
	}
}
