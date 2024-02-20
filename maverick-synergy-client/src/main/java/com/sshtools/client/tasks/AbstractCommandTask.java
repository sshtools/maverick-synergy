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

import com.sshtools.client.SessionChannelNG;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SshConnection;

/**
 * An abstract task for executing commands.
 */
@Deprecated(since = "3.1.0")
public abstract class AbstractCommandTask extends AbstractSessionTask<SessionChannelNG> {

	@Deprecated(since = "3.1.0", forRemoval = true)
	public static final int EXIT_CODE_NOT_RECEIVED = Integer.MIN_VALUE;
	
	private final String command;
	private final String charset;
	private final boolean autoConsume;

	protected AbstractCommandTask(AbstractSessionTaskBuilder<?, SessionChannelNG, ?> builder, String command, String charset, boolean autoConsume) {
		super(builder);
		this.command = command;
		this.charset = charset;
		this.autoConsume = autoConsume;
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	public AbstractCommandTask(SshConnection con, String command,
			String charset) {
		super(con);
		this.command = command;
		this.charset = charset;
		this.autoConsume = false;
	}
	
	@Deprecated(since = "3.1.0", forRemoval = true)
	public AbstractCommandTask(SshConnection con, String command,
			String charset, ChannelRequestFuture future) {
		super(con, future);
		this.command = command;
		this.charset = charset;
		this.autoConsume = false;
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	public AbstractCommandTask(SshConnection con, String command) {
		this(con, command, "UTF-8");
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	public AbstractCommandTask(SshConnection con, String command, ChannelRequestFuture future) {
		this(con, command, "UTF-8", future);
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	protected SessionChannelNG createSession(SshConnection con) {
		return new SessionChannelNG(
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(), 
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				getChannelFuture(), 
				autoConsume);
	}
	
	@Override
	@Deprecated(since = "3.1.0", forRemoval = true)
	protected void onCloseSession(SessionChannelNG session) {
	}

	public int getExitCode() {
		return getSession().getExitCode();
	}
	
	public String getCommand() {
		return command;
	}
	
	@Override
	protected final void setupSession(SessionChannelNG session) {
		beforeExecuteCommand(session);
		session.executeCommand(command, charset);
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	protected void beforeExecuteCommand(SessionChannelNG session) {

	}
	
}
