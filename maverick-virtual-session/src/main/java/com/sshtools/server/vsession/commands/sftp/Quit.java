package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import org.jline.reader.EndOfFileException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Quit extends SftpCommand {

	public Quit() {
		super("quit", "SFTP", "quit", "Quit the SFTP shell");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		throw new EndOfFileException();
	}

}
