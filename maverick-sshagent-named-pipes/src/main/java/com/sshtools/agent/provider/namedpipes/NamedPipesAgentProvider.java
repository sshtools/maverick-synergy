package com.sshtools.agent.provider.namedpipes;

import java.io.IOException;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;

public class NamedPipesAgentProvider implements AgentProvider {

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if(type == AgentSocketType.WINDOWS_NAMED_PIPE) {
			NamedPipeClient namedPipe = new NamedPipeClient(location);
			return new SshAgentClient(false, application, namedPipe, namedPipe.getInputStream(), namedPipe.getOutputStream(), false);
		}
		return null;
	}

	@SuppressWarnings("resource")
	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.WINDOWS_NAMED_PIPE) {
			return new NamedPipeAcceptor(new NamedPipeServer(location));
		}
		return null;
	}

}
