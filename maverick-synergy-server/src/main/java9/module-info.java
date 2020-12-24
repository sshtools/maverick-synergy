module com.sshtools.synergy.server {
	exports com.sshtools.server;
	exports com.sshtools.server.components;
	exports com.sshtools.server.components.jce;
	requires transitive com.sshtools.synergy.common;
	requires transitive com.sshtools.common.util;
}