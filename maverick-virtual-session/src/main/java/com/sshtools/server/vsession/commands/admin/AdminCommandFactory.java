package com.sshtools.server.vsession.commands.admin;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.jvm.Mem;
import com.sshtools.server.vsession.jvm.ThreadDump;
import com.sshtools.server.vsession.jvm.Threads;

public class AdminCommandFactory extends CommandFactory<ShellCommand> {

	public AdminCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("threads", Threads.class);
		commands.put("shutdown", Shutdown.class);
		commands.put("con", Connections.class);
		commands.put("mem", Mem.class);
		commands.put("threaddump", ThreadDump.class);
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
