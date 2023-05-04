package com.sshtools.agent.provider.jni;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;
import com.sshtools.agent.server.ServerSocketTransportAdapter;

public class JniUnixDomainSocketAgentProvider implements AgentProvider {

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			Socket socket = AFUNIXSocket.newInstance();
			File socketFile = new File(location);
			socket.connect(new AFUNIXSocketAddress(socketFile));
			return new SshAgentClient(false, application, socket, socket.getInputStream(), socket.getOutputStream(),
					false);
		}
		
		return null;
	}

	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			AFUNIXServerSocket server = AFUNIXServerSocket.newInstance(); 
			server.bind(new AFUNIXSocketAddress(new File(location)));
			return new ServerSocketTransportAdapter(server);
		}
		return null;
	}

}
