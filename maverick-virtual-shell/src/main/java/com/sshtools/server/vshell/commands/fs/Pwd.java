package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Pwd extends ShellCommand {
	public Pwd() {
		super("pwd", SUBSYSTEM_FILESYSTEM, "");
		setDescription("Returns the current working directory");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException, PermissionDeniedException {
		process.getConsole().printStringNewline(process.getCurrentDirectory().getAbsolutePath());
	}
}
