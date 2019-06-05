package com.sshtools.client.tasks;

import com.sshtools.client.AsyncSessionChannel;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.Connection;

public abstract class AsyncShellTask extends AbstractShellTask<AsyncSessionChannel> {

	public AsyncShellTask(Connection<SshClientContext> con) {
		super(con);
	}

	@Override
	protected void onOpenSession(AsyncSessionChannel session) {
	}

	@Override
	protected void onCloseSession(AsyncSessionChannel session) {
	}

	protected AsyncSessionChannel createSession(Connection<SshClientContext> con) {
		return new AsyncSessionChannel(
				con,
				con.getContext().getSessionMaxPacketSize(), 
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMinWindowSize(),
				future);
	}
}
