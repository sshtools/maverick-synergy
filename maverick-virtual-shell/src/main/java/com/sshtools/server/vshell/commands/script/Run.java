package com.sshtools.server.vshell.commands.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Run extends ShellCommand {
	public Run() {
		super("run", ShellCommand.SUBSYSTEM_FILESYSTEM, "<filename>", new Option("-l", "List supported script engines"));
		setDescription("Run a script (using one of the supported engines)");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException, PermissionDeniedException {
		String[] args = cli.getArgs();
		if (cli.hasOption('l')) {
			if (args.length > 1) {
				throw new IllegalArgumentException("No arguments should be supplied when listing engines (-l)");
			}
			
		}
		if (args.length < 2) {
			throw new IllegalArgumentException("At least one argument required");
		}
		for (int i = 1; i < args.length; i++) {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(args[i]);
			BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getInputStream()));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					process.getConsole().printStringNewline(line);
					process.getConsole().flushConsole();
				}
			} finally {
				reader.close();
			}
		}
	}
}
