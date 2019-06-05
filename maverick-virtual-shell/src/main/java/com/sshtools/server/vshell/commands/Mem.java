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
