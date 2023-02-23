/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.client.tasks;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SshConnection;

/**
 * An abstract task for executing commands.
 */
public abstract class AbstractCommandTask extends AbstractSessionTask<SessionChannelNG> {

	public static final int EXIT_CODE_NOT_RECEIVED = Integer.MIN_VALUE;
	
	String command;
	String charset = "UTF-8";
	int exitCode = EXIT_CODE_NOT_RECEIVED;
	
	public AbstractCommandTask(SshConnection con, String command,
			String charset) {
		super(con);
		this.command = command;
		this.charset = charset;
	}
	
	@SuppressWarnings("deprecation")
	public AbstractCommandTask(SshConnection con, String command,
			String charset, ChannelRequestFuture future) {
		super(con, future);
		this.command = command;
		this.charset = charset;
	}

	public AbstractCommandTask(SshConnection con, String command) {
		this(con, command, "UTF-8");
	}
	
	public AbstractCommandTask(SshConnection con, String command, ChannelRequestFuture future) {
		this(con, command, "UTF-8", future);
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
	
	@Override
	protected void onCloseSession(SessionChannelNG session) {
		exitCode = session.getExitCode();
	}

	public int getExitCode() {
		return exitCode;
	}
	
	public String getCommand() {
		return command;
	}
	
	@Override
	protected final void setupSession(SessionChannelNG session) {
		beforeExecuteCommand(session);
		session.executeCommand(command, charset);
	}

	protected void beforeExecuteCommand(SessionChannelNG session) {

	}
	
}
