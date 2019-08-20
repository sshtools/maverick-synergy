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
import java.util.Map;

import jline.ConsoleReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

public class Term extends ShellCommand {
	public Term() {
		super("term", SUBSYSTEM_SHELL, "[-k] [<termType>]", new Option("k",
				false, "Display all keycodes"), new Option("h", false,
				"Display keycodes as hex"), new Option("d", false,
				"Display keycodes as decimal"));
		setDescription("Displays information about terminal configuration, or sets the current terminal type");
	}

	public void run(CommandLine args, VirtualProcess process)
			throws IOException {
 		Console console = process.getConsole();
		if (args.hasOption('k')) {
			Map<?, ?> keyBindingsMap = console.getKeyBindingsMap();
			for (Object key : keyBindingsMap.keySet()) {
				Short keyCode = (Short) keyBindingsMap.get(key);
				String seq = key.toString();
				StringBuilder bui = new StringBuilder();
				for (int i = 0; i < seq.length(); i++) {
					char c = seq.charAt(i);
					if (bui.length() > 0) {
						bui.append(" ");
					}
					if (args.hasOption('h')) {
						bui.append("0x" + Integer.toHexString(c));
					} else if (args.hasOption('d')) {
						bui.append((int) c);
					} else {
						if (c < 32) {
							bui.append("^" + (char) (c + 64));
						} else if (c == 32) {
							bui.append("<Space>");
						} else if (c == 127) {
							bui.append("<Del>");
						} else if (c > 127 && c < 160) {
							bui.append("~" + (char) (c + 64));
						} else {
							bui.append(c);
						}
					}
				}
				console.printStringNewline(ConsoleReader.KEYMAP_CODES
						.get(keyCode.shortValue()) + "=" + bui);
			}
		} else {
			if (args.getArgList().size() == 1) {
				console.printStringNewline("TERM="
						+ process.getTerminal().getTerm());
				console.printStringNewline("WIDTH="
						+ console.getTerminal().getTerminalWidth());
				console.printStringNewline("HEIGHT="
						+ console.getTerminal().getTerminalHeight());
			} else {
				process.getTerminal().setTerminal(
						(String) args.getArgList().get(1));
			}
		}
	}
}
