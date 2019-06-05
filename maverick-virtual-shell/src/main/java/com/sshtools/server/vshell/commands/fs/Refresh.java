package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;


public class Refresh extends ShellCommand {
	public Refresh() {
		super("refresh", SUBSYSTEM_FILESYSTEM, "");
		setDescription("Refreshes the current directory");
		setBuiltIn(true);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		process.getCurrentDirectory().refresh();
	}
}
