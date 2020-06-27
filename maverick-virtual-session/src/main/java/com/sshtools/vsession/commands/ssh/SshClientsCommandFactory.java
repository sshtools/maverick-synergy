package com.sshtools.vsession.commands.ssh;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.commands.sftp.SftpClientCommand;

public class SshClientsCommandFactory extends CommandFactory<ShellCommand> {

	public SshClientsCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("ssh", SshClientCommand.class);
		commands.put("sftp", SftpClientCommand.class);
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}
}
