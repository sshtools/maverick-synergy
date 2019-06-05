package com.sshtools.client.tasks;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.Connection;

public abstract class ShellTask extends AbstractShellTask<SessionChannelNG> {

	public ShellTask(Connection<SshClientContext> con) {
		super(con);
	}

	@Override
	protected void onOpenSession(SessionChannelNG session) {
	}

	@Override
	protected void onCloseSession(SessionChannelNG session) {
	}

	protected SessionChannelNG createSession(Connection<SshClientContext> con) {
		return new SessionChannelNG(
				con,
				con.getContext().getSessionMaxPacketSize(), 
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMaxWindowSize(),
				con.getContext().getSessionMinWindowSize(),
				future);
	}
}
