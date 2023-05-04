/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
