package com.sshtools.common.ssh;

public interface SecureComponent {

	SecurityLevel getSecurityLevel();
	
	String getAlgorithm();
	
	int getPriority();
}
