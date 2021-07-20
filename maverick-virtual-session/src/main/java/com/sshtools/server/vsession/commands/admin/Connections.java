
package com.sshtools.server.vsession.commands.admin;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Connections extends ShellCommand {

	public Connections() {
		super("con", ShellCommand.SUBSYSTEM_SYSTEM, "con", "Show active connections");
	}

	public void run(String[] args, VirtualConsole process)
			throws IOException, PermissionDeniedException {

		process.println(String.format("%s %16s %s", "UUID", "IP Address", "Username"));
		for(SshConnection c : process.getConnection().getConnectionManager().getAllConnections()) {
			process.println(String.format("%s %16s %s", c.getUUID(), c.getRemoteAddress().getAddress(), c.getUsername()));
		}
	}

}
