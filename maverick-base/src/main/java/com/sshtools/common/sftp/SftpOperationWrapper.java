
package com.sshtools.common.sftp;

import com.sshtools.common.ssh.SessionChannel;

public interface SftpOperationWrapper {

	void onBeginOperation(SessionChannel session, SftpSubsystemOperation op);
	
	void onEndOperation(SessionChannel session, SftpSubsystemOperation op);
}
