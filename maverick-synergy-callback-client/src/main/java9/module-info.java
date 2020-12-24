module com.sshtools.callback.client {
	requires transitive com.sshtools.common.base;
	requires transitive com.sshtools.common.logger;
	requires transitive com.sshtools.synergy.common;
	requires transitive com.sshtools.synergy.server;
	exports com.sshtools.callback.client;
}