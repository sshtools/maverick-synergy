module com.sshtools.common.base {
	requires transitive com.sshtools.common.util;
	requires org.bouncycastle.provider;
	requires transitive com.sshtools.common.logger;
	
	exports com.sshtools.common.auth;
	exports com.sshtools.common.command;
	exports com.sshtools.common.events;
	exports com.sshtools.common.files;
	exports com.sshtools.common.files.direct;
	exports com.sshtools.common.files.nio;
	exports com.sshtools.common.forwarding;
	exports com.sshtools.common.knownhosts;
	exports com.sshtools.common.net;
	exports com.sshtools.common.nio;
	exports com.sshtools.common.permissions;
	exports com.sshtools.common.policy;
	exports com.sshtools.common.publickey;
	exports com.sshtools.common.publickey.authorized;
	exports com.sshtools.common.rsa;
	exports com.sshtools.common.scp;
	exports com.sshtools.common.sftp;
	exports com.sshtools.common.sftp.extensions;
	exports com.sshtools.common.sftp.extensions.filter;
	exports com.sshtools.common.shell;
	exports com.sshtools.common.ssh;
	exports com.sshtools.common.ssh.components;
	exports com.sshtools.common.ssh.components.jce;
	exports com.sshtools.common.ssh.compression;
	exports com.sshtools.common.ssh2;
	exports com.sshtools.common.sshd;
}