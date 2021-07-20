
package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Lpwd extends SftpCommand {

	public Lpwd() {
		super("lpwd", "SFTP", "lpwd", "Print out the local current working directory");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		console.println(console.getCurrentDirectory().getAbsolutePath());
	}


}
