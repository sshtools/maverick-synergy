/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
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
