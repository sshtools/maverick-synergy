package com.sshtools.client.tasks;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.Connection;

/**
 * An abstract task for executing commands.
 */
public abstract class AbstractCommandTask extends AbstractSessionTask<SessionChannelNG> {

	static final int EXIT_CODE_NOT_RECEIVED = Integer.MIN_VALUE;
	
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
	protected final void setupSession(SessionChannelNG session) {
		beforeExecuteCommand(session);
		session.executeCommand(command, charset);
	}

	protected void beforeExecuteCommand(SessionChannelNG session) {
		
	}
	
}
