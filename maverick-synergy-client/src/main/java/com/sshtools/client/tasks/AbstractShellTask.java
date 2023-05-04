package com.sshtools.client.tasks;

import com.sshtools.client.AbstractSessionChannel;
import com.sshtools.client.SshClient;
import com.sshtools.common.ssh.SshConnection;

/**
 * An abstract task for starting the shell.
 */
public abstract class AbstractShellTask<T extends AbstractSessionChannel> extends AbstractSessionTask<T> {

	public AbstractShellTask(AbstractConnectionTaskBuilder<?, ?> builder) {
		super(builder);
	}

	public AbstractShellTask(SshConnection con) {
		super(con);
	}
	
	public AbstractShellTask(SshClient ssh) {
		super(ssh.getConnection());
	}
	
	@Override
	protected final void setupSession(T session) {
		beforeStartShell(session);
		session.startShell();
	}

	protected void beforeStartShell(T session) {
		
	}
}
