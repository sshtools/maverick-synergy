
package com.sshtools.client.tasks;

import com.sshtools.client.SshClient;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;

public abstract class Task extends ConnectionAwareTask implements Runnable {

	public Task(SshConnection con) {
		super(con);
	}
	
	public Task(SshClient ssh) {
		super(ssh.getConnection());
	}

}
