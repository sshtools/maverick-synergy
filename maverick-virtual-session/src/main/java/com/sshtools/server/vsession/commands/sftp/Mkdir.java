package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Mkdir extends SftpCommand {

	public Mkdir() {
		super("mkdir", "SFTP", "mkdir", "Create remote directory specified by path.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length != 2) {
			throw new IllegalArgumentException("Too many arguments.");
		} else {
			try {
				this.sftp.mkdir(args[1]);
			} catch (SftpStatusException | SshException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}
}
