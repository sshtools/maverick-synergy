module com.sshtools.synergy.client {
	exports com.sshtools.client;
	exports com.sshtools.client.components;
	exports com.sshtools.client.events;
	exports com.sshtools.client.sftp;
	exports com.sshtools.client.shell;
	exports com.sshtools.client.tasks;
	requires transitive com.sshtools.common.base;
	requires transitive com.sshtools.common.util;
	requires transitive com.sshtools.synergy.common;
}