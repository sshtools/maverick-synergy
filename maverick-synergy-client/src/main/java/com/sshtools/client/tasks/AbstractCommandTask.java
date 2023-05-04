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
