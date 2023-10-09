
module com.sshtools.server.vsession {
	requires transitive org.jline;
	requires transitive com.sshtools.maverick.base;
	requires transitive commons.cli;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.synergy.server;
	requires org.apache.commons.lang3;
	requires pty4j;
	exports com.sshtools.server.vsession;
	exports com.sshtools.server.vsession.commands;
	exports com.sshtools.server.vsession.commands.admin;
	exports com.sshtools.server.vsession.commands.fs;
	exports com.sshtools.server.vsession.commands.os;
	exports com.sshtools.server.vsession.commands.sftp;
	exports com.sshtools.server.vsession.jvm;
	exports com.sshtools.vsession.commands.ssh;
}
