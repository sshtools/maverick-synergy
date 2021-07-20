
package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Cd extends SftpCommand {

	public Cd() {
		super("cd", "SFTP", "cd", "Moves the working directory to a new directory");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length > 2)
			throw new IllegalArgumentException("Too many arguments.");
		if (args.length > 1) {
			cdRemote(args[1]);
		} else {
			cdRemote(getRemoteHome());
		}
	}


	private void cdRemote(String directory) {
		try {
			this.sftp.cd(directory);
		} catch (SftpStatusException | SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	private String getRemoteHome() {
		try {
			return this.sftp.getHome();
		} catch (SftpStatusException | SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
