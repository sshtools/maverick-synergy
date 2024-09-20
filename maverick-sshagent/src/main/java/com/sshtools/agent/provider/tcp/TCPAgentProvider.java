package com.sshtools.agent.provider.tcp;

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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.ServerSocketTransportAdapter;
import com.sshtools.agent.server.SshAgentAcceptor;

public class TCPAgentProvider implements AgentProvider {

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if (type == AgentSocketType.TCPIP) {
			int idx = location.indexOf(":");
			if (idx == -1) {
				return null;
			}
			String host = location.substring(0, idx);
			int port = Integer.parseInt(location.substring(idx + 1));
			Socket socket = new Socket(host, port);
			return new SshAgentClient(false, application, socket, socket.getInputStream(), socket.getOutputStream(),
					false);
		}
		return null;
	}

	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.TCPIP) {
			int idx = location.indexOf(":");
			if (idx == -1) {
				return null;
			}
			String host = location.substring(0, idx);
			int port = Integer.parseInt(location.substring(idx + 1));
			ServerSocket socket = new ServerSocket();
			socket.bind(new InetSocketAddress(host, port));
			return new ServerSocketTransportAdapter(socket);
		}
		return null;
	}

}
