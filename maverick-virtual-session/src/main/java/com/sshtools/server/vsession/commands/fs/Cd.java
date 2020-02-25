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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: cd [directory]
 * @author lee
 */
public class Cd extends ShellCommand {
	public Cd() {
		super("cd", ShellCommand.SUBSYSTEM_FILESYSTEM, "cd [directory]","Moves the working directory to a new directory");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length > 2)
			throw new IllegalArgumentException("Too many arguments.");
		if (args.length > 1) {
			process.setCurrentDirectory(args[1]);
		} else {
			try {
				process.setCurrentDirectory(process.getEnvironment().getOrDefault("HOME", "").toString());
			} catch (PermissionDeniedException e) {
				throw new IllegalAccessError();
			}
		}
	}
}
