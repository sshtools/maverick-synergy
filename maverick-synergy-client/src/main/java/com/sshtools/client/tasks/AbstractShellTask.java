package com.sshtools.client.tasks;

import com.sshtools.client.AbstractSessionChannel;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.Connection;

/**
 * An abstract task for starting the shell.
 */
public abstract class AbstractShellTask<T extends AbstractSessionChannel> extends AbstractSessionTask<T> {

	public AbstractShellTask(Connection<SshClientContext> con) {
		super(con);
	}
	
	@Override
	protected final void setupSession(T session) {
		beforeStartShell(session);
		session.startShell();
	}

	protected void beforeStartShell(T session) {
		
	}
}
