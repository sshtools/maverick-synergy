
import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.provider.tcp.TCPAgentProvider;

open module com.sshtools.agent {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.util;
	requires com.sshtools.common.logger;
	exports com.sshtools.agent;
	exports com.sshtools.agent.client;
	exports com.sshtools.agent.exceptions;
	exports com.sshtools.agent.rfc;
	exports com.sshtools.agent.server;
	exports com.sshtools.agent.openssh;
	uses AgentProvider;
	provides AgentProvider with TCPAgentProvider;
}