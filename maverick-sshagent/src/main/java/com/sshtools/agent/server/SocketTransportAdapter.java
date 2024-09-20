package com.sshtools.agent.server;

/*-
 * #%L
 * Key Agent
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
