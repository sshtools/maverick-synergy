module com.sshtools.agent {
	requires transitive com.sshtools.common.base;
	requires transitive com.sshtools.common.util;
	requires transitive org.newsclub.net.unix;
	requires transitive com.sun.jna.platform;
	requires transitive com.sun.jna;
	requires com.sshtools.common.logger;
	exports com.sshtools.agent;
	exports com.sshtools.agent.client;
	exports com.sshtools.agent.exceptions;
	exports com.sshtools.agent.openssh;
	exports com.sshtools.agent.rfc;
	exports com.sshtools.agent.server;
	exports com.sshtools.agent.win32;
	
}