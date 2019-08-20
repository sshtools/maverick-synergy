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
package com.sshtools.server.vshell.terminal;

import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.Command;
import com.sshtools.server.vshell.CommandFactory;
import com.sshtools.server.vshell.VirtualProcess;

import jline.Completor;

public class CommandCompletor<T extends Command> implements Completor {
	final 
	public static ThreadLocal<Command> command = new ThreadLocal<Command>();

	private CommandFactory<T> commandFactory;
	private SshConnection con;
	private VirtualProcess process;

	public CommandCompletor(VirtualProcess process,
			CommandFactory<T> commandFactory, SshConnection con) {
		this.commandFactory = commandFactory;
		this.process = process;
	}

	public int complete(String buffer, int cursor, List<String> candidates) {
		// System.out.println("Buffer: " + buffer + " Cursor: " + cursor);
		for (String cmd : commandFactory.getSupportedCommands()) {
			if (buffer != null && buffer.equals(cmd)) {
				try {
					Command c = commandFactory.createCommand(cmd, con);
					c.init(process);
					command.set(c);
				} catch (Exception e) {
					Log.error("Failed to load command for completion.", e);
				}
				candidates.add(cmd);
			} else if (buffer == null || cmd.startsWith(buffer.trim())) {
				candidates.add(cmd);
			}
		}
		return 0;
	}
}
