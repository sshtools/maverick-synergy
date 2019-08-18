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

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.Connection;

/**
 * An abstract task for executing commands.
 */
public abstract class AbstractCommandTask extends AbstractSessionTask<SessionChannelNG> {

	public static final int EXIT_CODE_NOT_RECEIVED = Integer.MIN_VALUE;
	
	String command;
	String charset = "UTF-8";
	int exitCode = EXIT_CODE_NOT_RECEIVED;
	
	public AbstractCommandTask(Connection<SshClientContext> con, String command,
			String charset) {
		super(con);
		this.command = command;
		this.charset = charset;
	}
	
	public AbstractCommandTask(Connection<SshClientContext> con, String command,
			String charset, ChannelRequestFuture future) {
		super(con, future);
		this.command = command;
		this.charset = charset;
	}

	public AbstractCommandTask(Connection<SshClientContext> con, String command) {
		this(con, command, "UTF-8");
	}
	
	public AbstractCommandTask(Connection<SshClientContext> con, String command, ChannelRequestFuture future) {
		this(con, command, "UTF-8", future);
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
	
	@Override
	protected void onCloseSession(SessionChannelNG session) {
		exitCode = session.getExitCode();
	}

	public int getExitCode() {
		return exitCode;
	}
	
	@Override
	protected final void setupSession(SessionChannelNG session) {
		beforeExecuteCommand(session);
		session.executeCommand(command, charset);
	}

	protected void beforeExecuteCommand(SessionChannelNG session) {
		
	}
	
}
