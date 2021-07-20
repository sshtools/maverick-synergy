
package com.sshtools.server.vsession.jvm;

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
