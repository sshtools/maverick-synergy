
package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;


public class Refresh extends ShellCommand {
	public Refresh() {
		super("refresh", SUBSYSTEM_FILESYSTEM, "refresh", "Refreshes the current directory");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {
		process.getCurrentDirectory().refresh();
	}
}
