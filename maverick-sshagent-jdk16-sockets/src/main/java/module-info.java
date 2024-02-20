
import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.provider.jdk16.JDK16UnixDomainSocketAgentProvider;

open module com.sshtools.agent.jdk16 {
	requires transitive com.sshtools.agent;
	provides AgentProvider with JDK16UnixDomainSocketAgentProvider;
}
