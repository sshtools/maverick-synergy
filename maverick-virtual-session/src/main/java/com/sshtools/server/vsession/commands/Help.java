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
package com.sshtools.server.vsession.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.server.vsession.Command;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.ShellUtilities;
import com.sshtools.server.vsession.VirtualConsole;

public class Help<T extends AbstractFile> extends ShellCommand {

	public Help() {
		super("help", SUBSYSTEM_HELP, "[<command>]", "Display information about the available commands.");
	}

	public boolean isHidden() {
		return true;
	}
	
	public void run(String[] args, VirtualConsole console) throws IOException {
		java.util.Set<String> cmds = console.getShell().getCommandFactory().getSupportedCommands();

		if (args.length == 2 && cmds.contains(args[1])) {
			console.getContext().getPolicy(ShellPolicy.class).checkPermission(
					console.getConnection(), ShellPolicy.EXEC, args[1]);
			try {

				Command cmd = console.getShell().getCommandFactory().createCommand(args[1], console.getConnection());

				console.println(cmd.getUsage());
			} catch (Exception e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		} else {
			// Create a list of subsystems and their commands
			HashMap<String, Map<String, Command>> subsystems = new HashMap<String, Map<String, Command>>();
			Iterator<String> it = cmds.iterator();
			Command cmd;
			Map<String, Command> comandMap;

			while (it.hasNext()) {
				try {
					String cmdName = (String) it.next();

					if (console.getContext().getPolicy(ShellPolicy.class).checkPermission(
							console.getConnection(), ShellPolicy.EXEC,  cmdName)) {
						cmd = console.getShell().getCommandFactory().createCommand(cmdName, console.getConnection());

						if(!cmd.isHidden()) {
							if (!subsystems.containsKey(cmd.getSubsystem())) {
								comandMap = new HashMap<String, Command>();
								comandMap.put(cmd.getCommandName(), cmd);
								subsystems.put(cmd.getSubsystem(), comandMap);
							} else {
								comandMap = subsystems.get(cmd.getSubsystem());
								comandMap.put(cmd.getCommandName(), cmd);
							}
						}
					}
				} catch (Exception e) {
				}
			}

			console.println();
			console.println("The following commands are available:");
			console.println();
			Iterator<Map.Entry<String, Map<String, Command>>> subsystemsIterator = subsystems.entrySet().iterator();
			Map.Entry<String, Map<String, Command>> entry;
			while (subsystemsIterator.hasNext()) {
				entry = subsystemsIterator.next();
				console.println((String) entry.getKey() + " commands:");
				comandMap = entry.getValue();
				for (Command shellCmd : comandMap.values()) {
					console.println(ShellUtilities.padString("", 5)
						+ ShellUtilities.padString(shellCmd.getCommandName(), 30) + shellCmd.getDescription());
				}

				console.println();
			}
			
			console.println(ShellUtilities.padString("", 5)
					+ ShellUtilities.padString("help [command]", 15)
				    + "Display command signature.");
		}
	}
}
