
/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Desktop SSH Agent.
 *
 * Desktop SSH Agent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Desktop SSH Agent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Desktop SSH Agent.  If not, see <https://www.gnu.org/licenses/>.
 */package com.sshtools.agent.provider.namedpipes;

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
