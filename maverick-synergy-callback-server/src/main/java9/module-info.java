module com.sshtools.callback.server {
	exports com.sshtools.server.callback;
	exports com.sshtools.server.callback.commands;
	requires com.sshtools.synergy.common;
	requires com.sshtools.common.util;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.synergy.server;
	requires transitive com.sshtools.common.vsession;
	requires commons.vfs2;
	requires transitive com.sshtools.common.files.vfs;
	requires vfs.sftp;
}