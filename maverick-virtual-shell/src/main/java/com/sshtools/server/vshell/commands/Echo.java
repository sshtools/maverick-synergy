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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

/**
 * Usage: echo [-n] [string]
 * @author lee
 *
 */
public class Echo extends ShellCommand {

	public Echo() {
		super("echo", ShellCommand.SUBSYSTEM_SHELL, "echo [-n] [string]", new Option(
			"n", false, "Do not output a line feed"));
		setDescription("Echo a message to the screen");
		setBuiltIn(true);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		StringBuilder bui = new StringBuilder();
		String[] a = args.getArgs();
		for (int i = 1 ; i < a.length; i++) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			bui.append(a[i]);
		}
		if (args.hasOption('n')) {
			process.getConsole().printString(bui.toString());
		} else {
			process.getConsole().printStringNewline(bui.toString());
		}
	}
}
