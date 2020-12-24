module com.sshtools.synergy.common {
	requires transitive com.sshtools.common.base;
	requires transitive com.sshtools.common.logger;
	requires com.sshtools.common.util;
	requires java.xml;
	exports com.sshtools.synergy.common.nio;
	exports com.sshtools.synergy.common.nio.ssl;
	exports com.sshtools.synergy.common.ssh;
	exports com.sshtools.synergy.common.ssh.components;
	exports com.sshtools.synergy.common.ssh.components.jce;
	exports com.sshtools.synergy.common.util;
}