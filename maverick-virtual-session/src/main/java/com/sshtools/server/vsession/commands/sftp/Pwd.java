package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Pwd extends SftpCommand {

	public Pwd() {
		super("pwd", "SFTP", "pwd", "Print out the remote current working directory");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		console.println(this.sftp.pwd());
	}


}
