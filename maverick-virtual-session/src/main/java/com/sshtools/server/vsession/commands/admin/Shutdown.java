package com.sshtools.server.vsession.commands.admin;

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

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Shutdown extends ShellCommand {
	public Shutdown() {
		super("shutdown", SUBSYSTEM_JVM, "shutdown <exitValue>", "Exit the JVM");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process) throws IOException {

		if (args.length > 2) {
			throw new IOException("Incorrect number of arguments.");
		}
		System.exit(args.length == 1 ? 0 : Integer.parseInt(args[1]));
	}
}
