package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Env extends ShellCommand {

	public Env() {
		super("set", SUBSYSTEM_SHELL, "[<variable>=<value>]");
		setDescription("Set an environment variable");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException {
		String[] args = cli.getArgs();
		if (args.length == 1) {
			// Display all the environment variables
			Iterator<Map.Entry<String, Object>> it = process.getEnvironment().entrySet().iterator();
			Map.Entry<String, Object> entry;
			while (it.hasNext()) {
				entry = it.next();
				process.getConsole().printStringNewline((String) entry.getKey() + "=" + formatEntryValue(entry));
			}
		} else if (args.length == 2) {

			if (args[1].indexOf("=") > -1) {
				String name = args[1].substring(0, args[1].indexOf("="));
				String value = args[1].substring(args[1].indexOf("=") + 1);
				process.getEnvironment().put(name, value);
			}
		} else {
			process.getConsole().printStringNewline("ERR: Incorrect number of arguments. Use help [command] for signature.");
		}
	}

	protected Object formatEntryValue(Map.Entry<String, Object> entry) {
		if (entry.getValue() == null) {
			return "<null>";
		}
		return entry.getValue();
	}
}
