/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
