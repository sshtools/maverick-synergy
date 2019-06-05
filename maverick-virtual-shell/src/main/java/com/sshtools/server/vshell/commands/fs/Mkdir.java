package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Mkdir extends ShellCommand {
	public Mkdir() {
		super("mkdir", SUBSYSTEM_FILESYSTEM,
			"<name> [<name2> <name3> .. <nameX>]");
		setDescription("Create one or more directories");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException,
			PermissionDeniedException {
		String[] argStrings = args.getArgs();
		if (argStrings.length < 2)
			throw new IOException("At least one argument required");
		for (int i = 1; i < argStrings.length; i++) {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(argStrings[i]);
			obj.createFolder();
		}
	}
}
