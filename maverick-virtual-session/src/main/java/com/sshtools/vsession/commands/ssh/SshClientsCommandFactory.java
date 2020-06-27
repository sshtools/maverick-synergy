package com.sshtools.vsession.commands.ssh;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.commands.fs.Cat;
import com.sshtools.server.vsession.commands.fs.Cd;
import com.sshtools.server.vsession.commands.fs.Cp;
import com.sshtools.server.vsession.commands.fs.Follow;
import com.sshtools.server.vsession.commands.fs.Ls;
import com.sshtools.server.vsession.commands.fs.Mkdir;
import com.sshtools.server.vsession.commands.fs.Mv;
import com.sshtools.server.vsession.commands.fs.Nano;
import com.sshtools.server.vsession.commands.fs.Pwd;
import com.sshtools.server.vsession.commands.fs.Refresh;
import com.sshtools.server.vsession.commands.fs.Rm;
import com.sshtools.server.vsession.commands.sftp.SftpClientCommand;

public class SshClientsCommandFactory extends CommandFactory<ShellCommand> {

	public SshClientsCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("ssh", SshClientCommand.class);
		commands.put("sftp", SftpClientCommand.class)
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}
}
