import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.provider.namedpipes.NamedPipesAgentProvider;

open module com.sshtools.agent.namedpipes {
	requires transitive com.sshtools.agent;
	requires transitive com.sun.jna.platform;
	requires transitive com.sun.jna;
	provides AgentProvider with NamedPipesAgentProvider;
}