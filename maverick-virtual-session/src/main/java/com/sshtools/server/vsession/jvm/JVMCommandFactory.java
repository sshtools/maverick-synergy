
package com.sshtools.server.vsession.jvm;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;

public class JVMCommandFactory extends CommandFactory<ShellCommand> {

	public JVMCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("threads", Threads.class);
		commands.put("mem", Mem.class);
		commands.put("threaddump", ThreadDump.class);
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
