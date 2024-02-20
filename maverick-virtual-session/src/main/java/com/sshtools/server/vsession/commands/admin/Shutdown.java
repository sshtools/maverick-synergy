package com.sshtools.server.vsession.commands.admin;

import java.io.IOException;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Shutdown extends ShellCommand {
	public Shutdown() {
		super("shutdown", SUBSYSTEM_JVM, "shutdown <exitValue>", "Exit the JVM");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process) throws IOException {

		if (args.length > 2) {
			throw new IOException("Incorrect number of arguments.");
		}
		System.exit(args.length == 1 ? 0 : Integer.parseInt(args[1]));
	}
}
