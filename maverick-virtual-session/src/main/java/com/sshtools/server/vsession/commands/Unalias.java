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
import java.util.HashMap;
import java.util.Map;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: unalias [-a] name [name ...]
 * @author lee
 *
 */
public class Unalias extends ShellCommand {
	

	public Unalias() {
		super("unalias", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("unalias -a | <name>",
				"-a            Remove all aliases"), 
				"Unset an alias that has previously been set.");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		String username = console.getConnection().getUsername();


		Map<String, String> aliaslist;
		if (!Alias.userlist.containsKey(username)) {
			Alias.userlist.put(username, new HashMap<String, String>());
		}
		aliaslist = Alias.userlist.get(username);
		
		if (!CliHelper.hasShortOption(args, 'a') && args.length > 1) {
			boolean skip = true;
			for(String arg : args) {
				if(skip) {
					skip = false;
					continue;
				}
				if(aliaslist.containsKey(arg)) {
					aliaslist.remove(arg);
				} else {
					console.println("unalias: " + arg + ": not found");
				}
			}
			
		} else {
			if (Alias.userlist.containsKey(username)) {
				Alias.userlist.remove(username);
			} 
		} 
	}
}
