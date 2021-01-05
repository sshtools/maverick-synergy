/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Mv extends ShellCommand {
	public Mv() {
		super("mv", ShellCommand.SUBSYSTEM_FILESYSTEM, UsageHelper.build("mv [options] <sourcePath> <targetPath>",
				"-v        Verbose. Display file names as they are moved"), "Move or rename a files or directories");
	}

	public void run(String[] args, final VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length < 3)
			throw new IOException("Not enough file names supplied.");
		AbstractFile target = process.getCurrentDirectory().resolveFile(args[args.length - 1]);

		if (args.length > 3 && (!target.exists() || !target.isDirectory())) {
			throw new IOException("Target must exist as a folder if multiple sources are specified.");
		}

		for (int i = 1; i < args.length - 1; i++) {
			AbstractFile src = process.getCurrentDirectory().resolveFile(args[i]);
			if (src.isDirectory() && target.isFile()) {
				throw new IOException("Cannot move folder " + src + " to file " + target);
			}

			if (!target.exists()) {
				src.moveTo(target);
			} else {
				src.moveTo(target.resolveFile(src.getName()));
			}
			if (CliHelper.hasShortOption(args, 'v')) {
				process.println(src.toString());
			}
		}
	}
}
