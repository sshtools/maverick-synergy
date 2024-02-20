package com.sshtools.agent.provider.tcp;

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
