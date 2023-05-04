module com.sshtools.server.callback {
	requires transitive com.sshtools.maverick.base;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.synergy.server;
	requires transitive com.sshtools.common.files.vfs;
	requires transitive com.sshtools.server.vsession;
	requires vfs.sftp;
	exports com.sshtools.server.callback;
	exports com.sshtools.server.callback.commands;
}