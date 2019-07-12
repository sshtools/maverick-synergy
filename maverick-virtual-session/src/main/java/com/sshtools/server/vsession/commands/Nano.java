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
package com.sshtools.server.vsession.commands;

import java.io.IOException;
import java.nio.file.Paths;

import org.jline.builtins.Options;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Nano extends ShellCommand {

	public Nano() {
		super("nano", ShellCommand.SUBSYSTEM_TEXT_EDITING, "Usage: nano", "");
	}

	@Override
	public void run(String[] args, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		Options opt = Options.compile(args);
		org.jline.builtins.Nano edit = new org.jline.builtins.Nano(console.getTerminal(), Paths.get(""));
        edit.open(opt.args());
        edit.run();
	}

}
