package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Rmdir extends SftpCommand {

	public Rmdir() {
		super("rmdir", "SFTP", "rmdir", "Remove remote directory specified by path.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length != 2) {
			throw new IllegalArgumentException("Too many arguments.");
		} else {
			try {
				this.sftp.rm(args[1], true, true);
			} catch (SftpStatusException | SshException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}
}
