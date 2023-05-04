package com.sshtools.server.callback.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.callback.CallbackRegistrationService;
import com.sshtools.server.vsession.CommandFactory;

public class CallbackCommandFactory extends CommandFactory<CallbackCommand> {

	CallbackRegistrationService server;
	
	public CallbackCommandFactory(CallbackRegistrationService server) {
		this.server = server;
		installShellCommands();
	}
	
	protected void installShellCommands() {
		installCommand(Callbacks.class);
		//installCommand(CallbackShell.class); // Use ssh command instead. 
		installCommand(CallbackMount.class);
	}

	@Override
	protected void configureCommand(CallbackCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		c.setRegistrationService(server);
		super.configureCommand(c, con);
	}

}
