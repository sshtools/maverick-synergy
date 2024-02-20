package com.sshtools.server.vsession.commands;

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

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.AbstractUUIDCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Kill extends AbstractUUIDCommand {
	public Kill() {
		super("kill", SUBSYSTEM_SHELL, "kill [<loginId or processId>]", "Kill a process or login");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {

		if (args.length < 2) {
			throw new IOException("Not enough arguments.");
		} else {
			for (int i = 1; i < args.length; i++) {
				try {
					long pid = Long.parseLong(args[i]);
					console.getShell().killProcess(pid);
				}
				catch(NumberFormatException nfe) {
					SshConnection connection = console.getConnection().getConnectionManager().getConnectionById(args[i]);
					if(connection!=null)
						connection.disconnect("Killed by " + console.getConnection().getUsername());
				}
			}				
		}
	}
}
