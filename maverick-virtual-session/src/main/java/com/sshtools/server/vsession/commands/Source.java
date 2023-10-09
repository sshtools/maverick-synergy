package com.sshtools.server.vsession.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.VirtualConsole;

public class Source extends Msh {

	public Source() {
		super("source", SUBSYSTEM_SHELL, "source <script>", null);
		setDescription("Run script in same process");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		this.commandFactory = console.getShell().getCommandFactory();
		String[] args = cli.getArgs();
		if (args.length != 2) {
			throw new IllegalArgumentException("Expects a single script as the argument.");
		} else {
			source(console, console.getCurrentDirectory().resolveFile(args[1]));
		}
	}
}
