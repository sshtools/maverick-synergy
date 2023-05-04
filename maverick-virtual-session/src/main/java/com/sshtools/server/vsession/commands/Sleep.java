/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
