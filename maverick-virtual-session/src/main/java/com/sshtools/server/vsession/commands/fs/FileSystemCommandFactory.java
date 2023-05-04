package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;

public class FileSystemCommandFactory extends CommandFactory<ShellCommand> {

	public FileSystemCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("pwd", Pwd.class);
		commands.put("cd", Cd.class);
		commands.put("rm", Rm.class);
		commands.put("mv", Mv.class);
		commands.put("cp", Cp.class);
		commands.put("refresh", Refresh.class);
		commands.put("mkdir", Mkdir.class);
		commands.put("ls", Ls.class);
		commands.put("cat", Cat.class);
		commands.put("nano", Nano.class);
		commands.put("follow", Follow.class);
		
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
