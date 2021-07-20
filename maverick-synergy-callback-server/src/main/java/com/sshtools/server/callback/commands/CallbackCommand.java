
package com.sshtools.server.callback.commands;

import com.sshtools.server.callback.CallbackServer;
import com.sshtools.server.vsession.ShellCommand;

public abstract class CallbackCommand extends ShellCommand {

	protected CallbackServer server;
	
	public CallbackCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}
	
	public void setServer(CallbackServer server) { 
		this.server = server;
	}

}
