package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Rm extends ShellCommand {
	public Rm() {
		super("rm", ShellCommand.SUBSYSTEM_FILESYSTEM, "[<filePath>]");
		setDescription("Removes a file or directory");
		getOptions().addOption("r", false, "Recursively remove files and directories.");
		getOptions().addOption("v", false, "Verbose. Display file names as they are deleted.");
	}

	public void run(CommandLine cli, final VirtualProcess process) throws IOException, PermissionDeniedException {
		String[] args = cli.getArgs();
		if (args.length == 1)
			throw new IOException("No file names supplied.");

		for (int i = 1; i < args.length; i++) {
			delete(process, process.getCurrentDirectory().resolveFile(args[i]), cli.hasOption('r'), cli.hasOption('v'));
		}
	}
	
	
	private void delete(VirtualProcess process, AbstractFile file, boolean recurse, boolean verbose) throws IOException, PermissionDeniedException {
		
		if(file.isDirectory() && recurse) {
			List<AbstractFile> children = file.getChildren();
			for(AbstractFile f : children) {
				delete(process, f, true, verbose);
			}
		}
		file.delete(false);
		if(verbose) {
			try {
				process.getConsole().printStringNewline(file.getAbsolutePath());
			} catch (IOException e) {
			}			
		}
	}
}
