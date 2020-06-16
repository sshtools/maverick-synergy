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
