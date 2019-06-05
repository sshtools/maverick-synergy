/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Mkdir extends ShellCommand {
	public Mkdir() {
		super("mkdir", SUBSYSTEM_FILESYSTEM,
			"<name> [<name2> <name3> .. <nameX>]");
		setDescription("Create one or more directories");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException,
			PermissionDeniedException {
		String[] argStrings = args.getArgs();
		if (argStrings.length < 2)
			throw new IOException("At least one argument required");
		for (int i = 1; i < argStrings.length; i++) {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(argStrings[i]);
			obj.createFolder();
		}
	}
}
