package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Pwd extends ShellCommand {
	public Pwd() {
		super("pwd", SUBSYSTEM_FILESYSTEM, "pwd", "Returns the current working directory");
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {
		process.println(process.getCurrentDirectory().getAbsolutePath());
	}
}
