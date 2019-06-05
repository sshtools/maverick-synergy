package com.sshtools.server.vshell.commands.admin;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Shutdown extends ShellCommand {
	public Shutdown() {
		super("shutdown", SUBSYSTEM_JVM, "[<exitValue>]");
		setDescription("Exit the JVM");
		setBuiltIn(false);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException {
		String[] args = cli.getArgs();
		if (args.length > 2) {
			throw new IOException("Incorrect number of arguments.");
		}
		System.exit(args.length == 1 ? 0 : Integer.parseInt(args[1]));
	}
}
