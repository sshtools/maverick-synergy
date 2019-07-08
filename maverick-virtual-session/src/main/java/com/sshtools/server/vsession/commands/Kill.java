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
package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.AbstractUUIDCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Kill extends AbstractUUIDCommand {
	public Kill() {
		super("kill", SUBSYSTEM_SHELL, "[<loginId or processId>]", "Kill a process or login");
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
