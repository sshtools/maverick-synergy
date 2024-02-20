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
import java.util.Iterator;
import java.util.Map;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Env extends ShellCommand {

	public Env() {
		super("set", SUBSYSTEM_SHELL, "set <variable>=<value>", "Set an environment variable");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		
		if (args.length == 1) {
			// Display all the environment variables
			Iterator<Map.Entry<String, Object>> it = console.getEnvironment().entrySet().iterator();
			Map.Entry<String, Object> entry;
			while (it.hasNext()) {
				entry = it.next();
				console.println((String) entry.getKey() + "=" + formatEntryValue(entry));
			}
		} else if (args.length == 2) {

			if (args[1].indexOf("=") > -1) {
				String name = args[1].substring(0, args[1].indexOf("="));
				String value = args[1].substring(args[1].indexOf("=") + 1);
				console.getSessionChannel().setEnvironmentVariable(name, value);
			}
		} else {
			console.println("ERR: Incorrect number of arguments. Use help [command] for signature.");
		}
	}

	protected Object formatEntryValue(Map.Entry<String, Object> entry) {
		if (entry.getValue() == null) {
			return "<null>";
		}
		return entry.getValue();
	}
}
