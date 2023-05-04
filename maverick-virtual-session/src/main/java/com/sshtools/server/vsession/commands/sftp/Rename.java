package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Rename extends SftpCommand {

	public Rename() {
		super("rename", "SFTP", "rename", "Rename remote file from oldpath to newpath.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length != 3) {
			throw new IllegalArgumentException("Too many arguments.");
		} else {
			try {
				this.sftp.rename(args[1], args[2]);
			} catch (SftpStatusException | SshException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}
}
