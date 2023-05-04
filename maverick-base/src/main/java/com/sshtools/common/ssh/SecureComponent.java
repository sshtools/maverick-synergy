package com.sshtools.common.ssh;

import com.sshtools.common.ssh.components.Component;

public interface SecureComponent extends Component {

	SecurityLevel getSecurityLevel();
	
	String getAlgorithm();
	
	int getPriority();
}
