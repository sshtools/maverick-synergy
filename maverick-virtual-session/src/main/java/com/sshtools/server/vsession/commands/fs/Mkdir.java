package com.sshtools.server.vsession.commands.fs;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Mkdir extends ShellCommand {
	public Mkdir() {
		super("mkdir", SUBSYSTEM_FILESYSTEM,
			"mkdir <name>...", "Create one or more directories");
	}

	public void run(String[] args, VirtualConsole process) throws IOException,
			PermissionDeniedException {

		if (args.length < 2)
			throw new IOException("At least one argument required");
		for (int i = 1; i < args.length; i++) {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(args[i]);
			if(obj.exists()) {
				throw new FileNotFoundException(String.format("%s already exists", args[i]));
			}
			if(!obj.createFolder()) {
				throw new IOException(String.format("%s could not be created", args[i]));
			}
		}
	}
}
