package com.sshtools.agent.provider.jdk16;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;
import com.sshtools.agent.server.SshAgentTransport;

public class JDK16UnixDomainSocketAgentProvider implements AgentProvider {

	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			UnixDomainSocketAddress unixSocketAddress = UnixDomainSocketAddress.of(location);
			ServerSocketChannel serverSocket = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
	        serverSocket.bind(unixSocketAddress);
	        return new SshAgentAcceptor() {
				
				@Override
				public void close() throws IOException {
					serverSocket.close();
				}
				
				@Override
				public SshAgentTransport accept() throws IOException {
					SocketChannel socket = serverSocket.accept();
					return new SshAgentTransport() {
						
						@Override
						public OutputStream getOutputStream() throws IOException {
							return Channels.newOutputStream(socket);
						}
						
						@Override
						public InputStream getInputStream() throws IOException {
							return Channels.newInputStream(socket);
						}
						
						@Override
						public void close() throws IOException {
							socket.close();
						}
					};
				}
			};
		}
		return null;
	}

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			UnixDomainSocketAddress unixSocketAddress = UnixDomainSocketAddress.of(location);
			SocketChannel channel = SocketChannel.open(unixSocketAddress);
	        channel.configureBlocking(true);
			return new SshAgentClient(false, application, channel, Channels.newInputStream(channel), Channels.newOutputStream(channel),
					false);
		}
		return null;
	}

}
