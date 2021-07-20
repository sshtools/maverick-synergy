
package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Rm extends ShellCommand {
	public Rm() {
		super("rm", ShellCommand.SUBSYSTEM_FILESYSTEM, UsageHelper.build("rm [options] <path>",
				"-r         Recursively remove files and directories",
				"-v         Verbose. Display file names as they are deleted"), "Removes a file or directory");
	}

	public void run(String[] args, final VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length == 1)
			throw new IOException("No file names supplied.");

		for (int i = 1; i < args.length; i++) {
			delete(process, process.getCurrentDirectory().resolveFile(args[i]), 
					CliHelper.hasShortOption(args, 'r'), 
					CliHelper.hasShortOption(args,'v'));
		}
	}
	
	
	private void delete(VirtualConsole process, AbstractFile file, boolean recurse, boolean verbose) throws IOException, PermissionDeniedException {
		
		if(file.isDirectory() && recurse) {
			List<AbstractFile> children = file.getChildren();
			for(AbstractFile f : children) {
				delete(process, f, true, verbose);
			}
		}
		file.delete(false);
		if(verbose) {
			try {
				process.println(file.getAbsolutePath());
			} catch (IOException e) {
			}			
		}
	}
}
