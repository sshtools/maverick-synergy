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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.ShellCommandWithOptions;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: alias [-p] [name=[value] ...]
 * @author lee
 *
 */
public class Alias<T extends AbstractFile> extends ShellCommandWithOptions {
	public static Map<String, Map<String, String>> userlist = new HashMap<String, Map<String, String>>();
	public static Map<String, String> predefined = new HashMap<String, String>();

	public Alias() {
		super("alias", ShellCommand.SUBSYSTEM_SHELL, 
				"Usage: alias [-p] [name=[value] ...]",
				"Set an alias to abbreviate long commands.",
				new Option("p", false, "Print current values"));
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualConsole process) throws IOException {
		String username = process.getSessionChannel().getConnection().getUsername();

		Map<String, String> aliaslist;
		if (!userlist.containsKey(username)) {
			userlist.put(username, new HashMap<String, String>());
		}
		aliaslist = userlist.get(username);
		
		
		if (!cli.hasOption("p") && cli.getArgList().size() > 1) {
			boolean skip = true;
			for(String arg : cli.getArgList()) {
				if(skip) {
					skip = false;
					continue;
				}
				int idx = arg.indexOf('=');
				if(idx > -1) {
					String name = arg.substring(0, idx);
					String value = arg.substring(idx+1);
					
					if(name.equalsIgnoreCase("alias") || name.equalsIgnoreCase("unalias")) {
						process.println("alias: cannot use '" + name + "' as alias");
					} else {
						aliaslist.put(name, value);
					}
				} else {
					
					String value = aliaslist.get(arg);
					if(value==null) {
						process.println("alias: " + arg + ": not found");
					} else {
						process.println("alias " + arg + "='"+ value + "'");
					}
				}
			}
			
		} else {
			if (userlist.containsKey(username)) {
				Map.Entry<String, String> entry;
				Map<String, String> list = userlist.get(username);
				Iterator<Map.Entry<String, String>> it = list.entrySet()
					.iterator();
				while (it.hasNext()) {
					entry = it.next();
					process.println("alias " + entry.getKey() + "='"
						+ entry.getValue() + "'");
				}
			} else {
				process.println("No aliases set");
			}
		} 
	}

	public static void setPredefinedAlias(String alias, String cmd) {
		predefined.put(alias, cmd);
	}

	public static boolean hasAlias(String alias, String username) {
		boolean hasAlias = false;
		if (userlist.containsKey(username)) {
			Map<String, String> list = userlist.get(username);
			hasAlias = list.containsKey(alias);
		}

		return hasAlias || predefined.containsKey(alias);
	}

	public static String getAliasCommand(String alias, String username) {
		if (userlist.containsKey(username)) {
			Map<String, String> list = userlist.get(username);
			return list.get(alias);
		} else if (predefined.containsKey(alias)) {
			return predefined.get(alias);
		}
		return null;
	}



//	public List<String> getCollection(int i, VirtualConsole process) throws Exception {
//
//		List<String> l = null;
//
//		if (i == 2) {
//			Set<String> s = process.getMsh().getCommandFactory().getSupportedCommands();
//			String[] st = new String[s.size()];
//
//			process.getMsh().getCommandFactory().getSupportedCommands()
//				.toArray(st);
//			l = Arrays.asList(st);
//		}
//
//		return l;
//	}

}
