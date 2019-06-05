package com.sshtools.common.ssh;

public class ConnectionAwareTaskWrapper extends ConnectionAwareTask {

	Runnable r;
	public ConnectionAwareTaskWrapper(SshConnection con, Runnable r) {
		super(con);
		this.r = r;
	}

	@Override
	protected void doTask() {
		r.run();
	}

}
