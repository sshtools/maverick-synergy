package com.sshtools.common.ssh;

public interface SecurityPolicy {

	SecurityLevel getMinimumSecurityLevel();

	boolean isDropSecurityAsLastResort();

	void onIncompatibleSecurity(String hostname, 
			int port, 
			String remoteIdentification,
			IncompatibleAlgorithm... reports);

	boolean isManagedSecurity();


}
