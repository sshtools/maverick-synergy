
package com.sshtools.server.vsession.commands;

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
