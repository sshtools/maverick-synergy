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
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

public class Ps extends ShellCommand {
	public Ps() {
		super("ps", SUBSYSTEM_SHELL, "[-a]", new Option("a", false, "Show processes run by other users"));
		setDescription("List all running processes");
		setBuiltIn(false);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		if(args.hasOption('a')) {
			Collection<VirtualProcess> rootProcesses = process.getProcessFactory().getRootProcesses();
			synchronized(rootProcesses) {
				for(VirtualProcess p : rootProcesses) {
					printProcess(process, p, 1);
				}
			}
		}
		else {
			VirtualProcess root = process.getRootProcess();
			printProcess(process, root, 1);
		}
	}
	
	void printProcess(VirtualProcess thisProcess, VirtualProcess processToPrint, int indent) throws IOException {
		Console console = thisProcess.getConsole();
		String commandName = processToPrint.getCommand().getCommandName();
		String fmt = "%" + indent + "s%-15d %" + ( Math.max(30 - commandName.length(), 10) ) + "s";
		console.printStringNewline(String.format(fmt, "", processToPrint.getPID(),commandName));
		for(VirtualProcess process : processToPrint.getChildren()) {
			printProcess(thisProcess, process, indent + 1);
		}
	}
}
