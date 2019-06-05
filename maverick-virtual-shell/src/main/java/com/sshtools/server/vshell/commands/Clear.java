package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

/**
 * Usage: clear
 * @author lee
 *
 */
public class Clear extends ShellCommand {

	public Clear() {
		super("clear", ShellCommand.SUBSYSTEM_SHELL);
		setDescription("Clear the screen");
		setBuiltIn(true);
	}

	public void run(CommandLine args, VirtualProcess process)
			throws IOException, PermissionDeniedException {
		process.getConsole().clearScreen();
	}

}
