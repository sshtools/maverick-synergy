/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.server.vsession.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.server.vsession.Command;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.ShellUtilities;
import com.sshtools.server.vsession.VirtualConsole;

public class Help<T extends AbstractFile> extends ShellCommand {

	public Help() {
		super("help", SUBSYSTEM_HELP, "help <command>", "Display information about the available commands.");
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
				List<Command> values = new ArrayList<>(comandMap.values());
				Collections.sort(values, new Comparator<Command>() {
					@Override
					public int compare(Command o1, Command o2) {
						return o1.getCommandName().compareTo(o2.getCommandName());
					}
				});
				
				for (Command shellCmd : values) {
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
