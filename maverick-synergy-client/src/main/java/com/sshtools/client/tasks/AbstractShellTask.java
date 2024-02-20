package com.sshtools.client.tasks;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
