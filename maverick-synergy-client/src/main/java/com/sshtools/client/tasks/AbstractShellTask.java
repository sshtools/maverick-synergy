package com.sshtools.client.tasks;

import com.sshtools.client.AbstractSessionChannel;

/**
 * An abstract task for starting the shell.
 */
public abstract class AbstractShellTask<T extends AbstractSessionChannel> extends AbstractSessionTask<T> {

	protected AbstractShellTask(AbstractSessionTaskBuilder<?, T, ?> builder) {
		super(builder);
	}
	
	@Override
	protected final void setupSession(T session) {
		beforeStartShell(session);
		session.startShell();
	}

	protected void beforeStartShell(T session) {
		
	}
}
