/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Sleep extends ShellCommand {

	public Sleep() {
		super("sleep", ShellCommand.SUBSYSTEM_SHELL, "[-M|-s|-m|-h|-d] <time>");
		setDescription("Sleep for some time (defaults to seconds)");
		setBuiltIn(false);
	}

	public void run(CommandLine args, VirtualProcess process)
			throws IOException {
		String[] a = args.getArgs();
		if (a.length != 2) {
			throw new IllegalArgumentException(
					"Requires single argument specifying time to sleep.");
		}
		long mult = 1000;
		String ts = a[1];
		char t = ts.charAt(ts.length() - 1);
		if (t == 'M') {
			ts = ts.substring(ts.length() - 1);
			mult = 1;
		} else if (t == 's') {
			ts = ts.substring(ts.length() - 1);
			mult = 1;
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
			process.getConsole().printStringNewline("Interrupted");
		}
	}
}
