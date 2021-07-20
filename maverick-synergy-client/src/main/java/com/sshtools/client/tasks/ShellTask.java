
package com.sshtools.client.tasks;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.SshConnection;

public abstract class ShellTask extends AbstractShellTask<SessionChannelNG> {

	public ShellTask(SshConnection con) {
		super(con);
	}

	public ShellTask(SshClient ssh) {
		super(ssh);
	}

	protected void beforeStartShell(SessionChannelNG session) {
		session.allocatePseudoTerminal("dumb", 1000, 99);
	}
	
	@Override
	protected void onCloseSession(SessionChannelNG session) {
	}

	protected SessionChannelNG createSession(SshConnection con) {
		return new SessionChannelNG(
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(), 
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				future,
				false);
	}
}
