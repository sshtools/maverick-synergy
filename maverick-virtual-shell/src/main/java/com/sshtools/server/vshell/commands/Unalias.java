package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

/**
 * Usage: unalias [-a] name [name ...]
 * @author lee
 *
 */
public class Unalias extends ShellCommand {
	

	public Unalias() {
		super("unalias", ShellCommand.SUBSYSTEM_SHELL, "unalias [-a] name [name ...]", new Option("a", false,
				"Print current values"));
		setDescription("Set an alias to abbreviate long commands.");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException {
		String username = process.getSessionChannel().getConnection().getUsername();
		Console console = process.getConsole();

		Map<String, String> aliaslist;
		if (!Alias.userlist.containsKey(username)) {
			Alias.userlist.put(username, new HashMap<String, String>());
		}
		aliaslist = Alias.userlist.get(username);
		
		String[] args = cli.getArgs();
		if (!cli.hasOption('a') && args.length > 1) {
			boolean skip = true;
			for(String arg : args) {
				if(skip) {
					skip = false;
					continue;
				}
				if(aliaslist.containsKey(arg)) {
					aliaslist.remove(arg);
				} else {
					console.printStringNewline("unalias: " + arg + ": not found");
				}
			}
			
		} else {
			if (Alias.userlist.containsKey(username)) {
				Alias.userlist.remove(username);
			} 
		} 
	}
}
