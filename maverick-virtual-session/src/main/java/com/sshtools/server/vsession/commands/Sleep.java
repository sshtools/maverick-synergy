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

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Sleep extends ShellCommand {

	public Sleep() {
		super("sleep", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("sleep [options] <time>",
				"-M     Time argument is in milliseconds", 
				"-s     Time argument is in seconds (default)", 
				"-m     Time argument is in minutes",
				"-h     Time argument is in hours",
				"-d     Time argument is in days"), "Sleep for some time (defaults to seconds)");
		
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {

		if (args.length < 2) {
			throw new IllegalArgumentException(
					"Requires single argument specifying time to sleep.");
		}
		long mult = 1000;
		String ts = args[1];
		char t = ts.charAt(ts.length() - 1);
		if (t == 'M') {
			ts = ts.substring(ts.length() - 1);
			mult = 1;
		} else if (t == 's') {
			ts = ts.substring(ts.length() - 1);
			mult = 1000;
		} else if (t == 'm') {
			ts = ts.substring(ts.length() - 1);
			mult = 60000;
		} else if (t == 'h') {
			ts = ts.substring(ts.length() - 1);
			mult = 3600000;
		} else if (t == 'd') {
			ts = ts.substring(ts.length() - 1);
			mult = 3600000 * 24;
		}
		try {
			Thread.sleep(Long.parseLong(ts) * mult);
		} catch (Exception e) {
			console.println("Interrupted");
		}
	}
}
