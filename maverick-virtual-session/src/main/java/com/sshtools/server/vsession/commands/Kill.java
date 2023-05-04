package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.AbstractUUIDCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Kill extends AbstractUUIDCommand {
	public Kill() {
		super("kill", SUBSYSTEM_SHELL, "kill [<loginId or processId>]", "Kill a process or login");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {

		if (args.length < 2) {
			throw new IOException("Not enough arguments.");
		} else {
			for (int i = 1; i < args.length; i++) {
				try {
					long pid = Long.parseLong(args[i]);
					console.getShell().killProcess(pid);
				}
				catch(NumberFormatException nfe) {
					SshConnection connection = console.getConnection().getConnectionManager().getConnectionById(args[i]);
					if(connection!=null)
						connection.disconnect("Killed by " + console.getConnection().getUsername());
				}
			}				
		}
	}
}
