package com.sshtools.server.callback.commands;

import com.sshtools.server.callback.CallbackRegistrationService;
import com.sshtools.server.vsession.ShellCommand;

public abstract class CallbackCommand extends ShellCommand {

	protected CallbackRegistrationService service;
	
	public CallbackCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}
	
	public void setRegistrationService(CallbackRegistrationService service) { 
		this.service = service;
	}

}
