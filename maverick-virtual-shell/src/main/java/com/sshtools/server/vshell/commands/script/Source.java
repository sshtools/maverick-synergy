package com.sshtools.server.vshell.commands.script;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.Msh;
import com.sshtools.server.vshell.VirtualProcess;

public class Source extends Msh {

	public Source() {
		super("source", SUBSYSTEM_SHELL, "[<script>]", null);
		setDescription("Run script in same process");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException, PermissionDeniedException {
		
		String[] args = cli.getArgs();
		if (args.length != 2) {
			throw new IllegalArgumentException("Expects a single script as the argument.");
		} else {
			source(process, process.getCurrentDirectory().resolveFile(args[1]));
		}
	}
}
