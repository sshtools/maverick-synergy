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
