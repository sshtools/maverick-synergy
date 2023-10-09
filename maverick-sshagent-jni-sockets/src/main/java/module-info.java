
import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.provider.jni.JniUnixDomainSocketAgentProvider;

open module com.sshtools.agent.jni {
	requires transitive com.sshtools.agent;
	requires transitive org.newsclub.net.unix;
	provides AgentProvider with JniUnixDomainSocketAgentProvider;
}