
package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.server.vsession.Environment;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Lcd extends SftpCommand {

	public Lcd() {
		super("lcd", "SFTP", "lcd", "Moves the working directory to a new directory");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length > 2)
			throw new IllegalArgumentException("Too many arguments.");
		if (args.length > 1) {
			cdLocal(args[1]);
		} else {
			cdLocal((String) console.getEnvironment().getOrDefault(Environment.ENV_HOME, ""));
		}
	}


	private void cdLocal(String directory) {
		try {
			this.sftp.lcd(directory);
		} catch (IOException | PermissionDeniedException | SftpStatusException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
}
