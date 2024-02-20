package com.sshtools.synergy.ssh;

import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;

public class ConnectionTaskWrapper extends ConnectionAwareTask {

	Runnable r;
	public ConnectionTaskWrapper(SshConnection con, Runnable r) {
		super(con);
		this.r = r;
	}

	@Override
	protected void doTask() {
		r.run();
	}

}
