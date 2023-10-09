package com.sshtools.server.vsession.commands.os;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;

public class OSCommandFactory extends CommandFactory<ShellCommand> {

	public OSCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("osshell", Shell.class);
		
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
