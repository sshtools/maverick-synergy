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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: cd [directory]
 * @author lee
 */
public class Cd extends ShellCommand {
	public Cd() {
		super("cd", ShellCommand.SUBSYSTEM_FILESYSTEM, "cd <directory>", "Moves the working directory to a new directory");
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
