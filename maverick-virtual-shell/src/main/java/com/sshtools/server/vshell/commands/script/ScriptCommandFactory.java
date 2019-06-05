package com.sshtools.server.vshell.commands.script;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.CommandFactory;
import com.sshtools.server.vshell.ShellCommand;


public class ScriptCommandFactory extends CommandFactory<ShellCommand> {

	public ScriptCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {

		commands.put("bsh", Bsh.class);
		commands.put("source", Source.class);
		commands.put("run", Run.class);
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
