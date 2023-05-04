package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Chgrp extends SftpCommand {

	public Chgrp() {
		super("chgrp", "SFTP", "chgrp", "Change group of file path to grp.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length != 3) {
			throw new IllegalArgumentException("Too many arguments, please note -h option not supported.");
		} else {
			try {
				this.sftp.chgrp(args[1], args[2]);
			} catch (SftpStatusException | SshException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}
	
}
