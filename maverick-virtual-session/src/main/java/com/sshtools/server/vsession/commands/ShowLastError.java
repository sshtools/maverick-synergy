package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class ShowLastError extends ShellCommand {

	public ShowLastError() {
		super("error", ShellCommand.SUBSYSTEM_SHELL, "error", "Display the last error");
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		Msh shell = console.getShell();
		if (shell.getLastError() != null) {
			console.println(
				"Message: " + shell.getLastError().getMessage());
		} else {
			console.println("No error to report");
		}
	}
}
