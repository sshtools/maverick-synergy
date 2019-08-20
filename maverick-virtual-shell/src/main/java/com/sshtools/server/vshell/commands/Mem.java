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
package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Mem extends ShellCommand {
	public Mem() {
		super("mem", SUBSYSTEM_JVM, "");
		setDescription("Displays JVM memory information");
		getOptions().addOption("m", "mb", false, "Display size in megabytes");
		getOptions().addOption("k", "kb", false, "Display size in kilobytes");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		Map<String, Long> memory = new HashMap<String, Long>();
		long free = Runtime.getRuntime().freeMemory();
		long max = Runtime.getRuntime().maxMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory();
		memory.put("free", free);
		memory.put("max", max);
		memory.put("total", total);
		memory.put("used", used);
		List<?> argList = args.getArgList();
		argList.remove(0);
		for (String name : memory.keySet()) {
			if (argList.size() == 0 || argList.contains(name)) {
				process.getConsole().printStringNewline(name + "=" + format(args, memory.get(name)));
			}
		}
	}

	static String format(CommandLine args, long value) {
		if (args.hasOption('m')) {
			return (value / 1024 / 1024) + " MB";
		} else if (args.hasOption('k')) {
			return (value / 1024) + " KB";
		}
		return value + " Bytes";
	}
}
