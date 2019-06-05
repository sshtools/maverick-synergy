package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.VirtualProcess;

public class Kill<T extends AbstractFile> extends AbstractUUIDCommand<T> {
	public Kill() {
		super("kill", SUBSYSTEM_SHELL, "[<loginId or processId>]");
		setDescription("Kill a process or login");
		setBuiltIn(false);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException {
		String[] args = cli.getArgs();
		if (args.length < 2) {
			throw new IOException("Not enough arguments.");
		} else {
			for (int i = 1; i < args.length; i++) {
				try {
					long pid = Long.parseLong(args[i]);
					process.killProcess(pid);
				}
				catch(NumberFormatException nfe) {
					SshConnection connection = process.getConnection().getConnectionManager().getConnectionById(args[i]);
					if(connection!=null)
						connection.disconnect("Killed by " + process.getSessionChannel().getConnection().getUsername());
				}
			}				
		}
	}
}
