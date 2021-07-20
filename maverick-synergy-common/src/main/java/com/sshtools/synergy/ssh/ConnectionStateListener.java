
package com.sshtools.synergy.ssh;

import com.sshtools.common.ssh.SshConnection;

public interface ConnectionStateListener {
	
	default public void connected(SshConnection con) {
		
	}
	
	default public void disconnected(SshConnection con) {
		
	}

	

}
