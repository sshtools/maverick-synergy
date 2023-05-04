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
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Date extends ShellCommand {

	public Date() {
		super("date", ShellCommand.SUBSYSTEM_SHELL, 
				UsageHelper.build("date [options]",
						"-t   Just output time",
						"-d   Just output date",
						"-l   Output date and time in long format",
						"-f   Use a custom format"), 
				"Display the current date");
		
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException, UsageException {
		DateFormat fmt = CliHelper.hasShortOption(args, 'l') ? DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG) : DateFormat
			.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		if (CliHelper.hasShortOption(args, 't')) {
			if (CliHelper.hasShortOption(args, 'l')) {
				fmt = DateFormat.getTimeInstance(DateFormat.LONG);
			} else {
				fmt = DateFormat.getTimeInstance(DateFormat.SHORT);
			}
		} else if (CliHelper.hasShortOption(args, 'd')) {
			if (CliHelper.hasShortOption(args, 'l')) {
				fmt = DateFormat.getDateInstance(DateFormat.LONG);
			} else {
				fmt = DateFormat.getDateInstance(DateFormat.SHORT);
			}
		} else if (CliHelper.hasShortOption(args, 'f')) {
			fmt = new SimpleDateFormat(CliHelper.getShortValue(args, 'f'));
		}
		console.println(fmt.format(new java.util.Date()));
	}
}
