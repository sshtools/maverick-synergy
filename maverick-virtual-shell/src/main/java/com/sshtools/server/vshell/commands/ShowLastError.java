package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.Msh;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class ShowLastError extends ShellCommand {

	public ShowLastError() {
		super("error", ShellCommand.SUBSYSTEM_SHELL, "");
		setDescription("Display the last error");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		Msh shell = (Msh) process.getParent().getCommand();
		if (shell.getLastError() != null) {
			process.getConsole().printStringNewline(
				"Message: " + shell.getLastError().getMessage());
		} else {
			process.getConsole().printStringNewline("No error to report");
		}
	}
}
