/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
