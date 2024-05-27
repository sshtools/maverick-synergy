
import com.sshtools.common.ssh.compression.SshCompressionFactory;

@SuppressWarnings("rawtypes")
module com.sshtools.synergy.common {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.synergy.nio;
	exports com.sshtools.synergy.nio.ssl;
	exports com.sshtools.synergy.ssh;
	exports com.sshtools.synergy.ssh.components;
	exports com.sshtools.synergy.ssh.components.jce;
	exports com.sshtools.synergy.util;
	opens com.sshtools.synergy.ssh;
	
	uses SshCompressionFactory;
}
