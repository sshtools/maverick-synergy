package com.sshtools.agent;

import java.io.IOException;

import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;

public interface AgentProvider {

	SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException;

	SshAgentAcceptor server(String location, AgentSocketType type) throws IOException;
}
