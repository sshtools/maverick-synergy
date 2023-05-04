package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: clear
 * @author lee
 *
 */
public class Clear extends ShellCommand {

	public Clear() {
		super("clear", ShellCommand.SUBSYSTEM_SHELL, "clear", "Clears the screen");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException {
		console.clear();
	}

}
