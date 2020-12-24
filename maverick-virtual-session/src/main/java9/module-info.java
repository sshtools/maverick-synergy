module com.sshtools.common.vsession {
	requires transitive org.jline;
	requires transitive com.sshtools.common.base;
	requires transitive commons.cli;
	requires com.sshtools.common.logger;
	requires com.sshtools.common.util;
	requires com.sshtools.synergy.common;
	requires transitive com.sshtools.synergy.server;
	requires transitive com.sshtools.synergy.client;
	exports com.sshtools.server.vsession.commands;
	exports com.sshtools.server.vsession;
	exports com.sshtools.server.vsession.commands.admin;
	exports com.sshtools.server.vsession.commands.fs;
	exports com.sshtools.server.vsession.commands.sftp;
	exports com.sshtools.server.vsession.jvm;
	
}