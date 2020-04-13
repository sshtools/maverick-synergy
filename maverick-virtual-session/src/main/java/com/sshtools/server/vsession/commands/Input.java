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
package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Input extends ShellCommand {

	public Input() {
		super("input", ShellCommand.SUBSYSTEM_SHELL,
				UsageHelper.build("input <env> <prompt>"),
				 "Read a line of input from the user and place it into an environment variable");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException, UsageException {
		if(args.length < 2) {
			throw new UsageException("You must provde an environment variable name to place the user input into!");
		}
		String val = console.readLine(getPrompt(args));
		console.getEnvironment().put(args[1], val);
	}

	private String getPrompt(String[] args) {
		StringBuffer buf = new StringBuffer();
		for(int i=2;i<args.length;i++) {
			buf.append(args[i]);
			buf.append(" ");
		}
		return buf.toString();
	}	
}
