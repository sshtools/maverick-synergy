
package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Input extends ShellCommand {

	public Input() {
		super("input", ShellCommand.SUBSYSTEM_SHELL,
				UsageHelper.build("input <env> <prompt>"),
				 "Read a line of input from the user and place it into an environment variable");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException, UsageException {
		if(args.length < 2) {
			throw new UsageException("You must provde an environment variable name to place the user input into!");
		}
		String val = console.readLine(getPrompt(args));
		console.getEnvironment().put(args[1], val);
	}

	private String getPrompt(String[] args) {
		StringBuffer buf = new StringBuffer();
		for(int i=2;i<args.length;i++) {
			buf.append(args[i]);
			buf.append(" ");
		}
		return buf.toString();
	}	
}
