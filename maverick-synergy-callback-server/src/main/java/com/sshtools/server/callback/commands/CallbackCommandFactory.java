
package com.sshtools.server.callback.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.callback.CallbackServer;
import com.sshtools.server.vsession.CommandFactory;

public class CallbackCommandFactory extends CommandFactory<CallbackCommand> {

	CallbackServer server;
	
	public CallbackCommandFactory(CallbackServer server) {
		this.server = server;
		installShellCommands();
	}
	
	protected void installShellCommands() {
		installCommand(Callbacks.class);
		installCommand(CallbackShell.class);
		installCommand(CallbackMount.class);
	}

	@Override
	protected void configureCommand(CallbackCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		c.setServer(server);
		super.configureCommand(c, con);
	}

}
