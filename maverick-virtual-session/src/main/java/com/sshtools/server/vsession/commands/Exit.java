
package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: exit
 * @author lee
 *
 */
public class Exit extends ShellCommand {

	public Exit() {
		super("exit", ShellCommand.SUBSYSTEM_SHELL, "exit", "Exits the current shell");
		setDescription("Exits the current shell");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		console.getShell().exit();
	}
}
