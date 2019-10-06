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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.tasks;

import java.io.IOException;

import com.sshtools.client.AsyncSessionChannel;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;

public abstract class AsyncShellTask extends AbstractShellTask<AsyncSessionChannel> {

	public AsyncShellTask(Connection<SshClientContext> con) {
		super(con);
	}
	
	public AsyncShellTask(SshClient ssh) {
		super(ssh.getConnection());
	}

	@Override
	protected void onOpenSession(AsyncSessionChannel session) throws IOException, SshException, ShellTimeoutException {
	}

	@Override
	protected void onCloseSession(AsyncSessionChannel session) {

	}

	protected AsyncSessionChannel createSession(SshConnection con) {
		return new AsyncSessionChannel(
				con,
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(), 
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				future);
	}
}
