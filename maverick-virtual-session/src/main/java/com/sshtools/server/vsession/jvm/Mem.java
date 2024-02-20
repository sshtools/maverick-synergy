package com.sshtools.server.vsession.jvm;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Mem extends ShellCommand {
	public Mem() {
		super("mem", SUBSYSTEM_JVM, "mem", "Displays JVM memory information");
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		Map<String, Long> memory = new HashMap<String, Long>();
		long free = Runtime.getRuntime().freeMemory();
		long max = Runtime.getRuntime().maxMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory();
		memory.put("free", free);
		memory.put("max", max);
		memory.put("total", total);
		memory.put("used", used);
		List<String> argList = new ArrayList<>(Arrays.asList(args));
		argList.remove(0);
		
		boolean filtered = argList.contains("free") || argList.contains("max") || argList.contains("total") || argList.contains("used");
		for (String name : memory.keySet()) {
			if (!filtered || argList.contains(name)) {
				console.println(name + "=" + format(args, memory.get(name)));
			}
		}
	}

	static String format(String[] args, long value) {
		if (CliHelper.hasShortOption(args, 'm')) {
			return (value / 1024 / 1024) + " MB";
		} else if (CliHelper.hasShortOption(args, 'k')) {
			return (value / 1024) + " KB";
		}
		return value + " Bytes";
	}
}
