package com.sshtools.server.vsession.commands.fs;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
