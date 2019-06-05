package com.sshtools.server.vshell.commands.admin;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.CommandFactory;
import com.sshtools.server.vshell.ShellCommand;



public class AdminCommandFactory extends CommandFactory<ShellCommand> {

	public AdminCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("threads", Threads.class);
		commands.put("shutdown", Shutdown.class);
		commands.put("con", Connections.class);
		commands.put("pref", Pref.class);
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
