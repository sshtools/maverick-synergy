package com.sshtools.client.tasks;

import com.sshtools.client.AbstractSessionChannel;
import com.sshtools.client.SshClient;
import com.sshtools.common.ssh.SshConnection;

/**
 * An abstract task for starting the shell.
 */
public abstract class AbstractShellTask<T extends AbstractSessionChannel> extends AbstractSessionTask<T> {

	protected AbstractShellTask(AbstractSessionTaskBuilder<?, T, ?> builder) {
		super(builder);
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	public AbstractShellTask(SshConnection con) {
		super(con);
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
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
