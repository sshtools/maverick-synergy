package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.synergy.ssh.TerminalModes;
import com.sshtools.synergy.ssh.TerminalModes.Mode;

public class Term extends ShellCommand {

	public Term() {
		super("term",ShellCommand.SUBSYSTEM_SHELL, 
				UsageHelper.build("term"),
				"Output information about the pseudo terminal");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		TerminalModes modes = console.getPseudoTerminalModes();
		console.printfln("Type: %s", console.getTerminal().getType());
		console.println("Modes");
		console.println("----------------");
		for(Mode mode : modes.modes().keySet()) {
			console.printfln("%s: 0x%s", StringUtils.rightPad(mode.name(), 13), Integer.toHexString(modes.get(mode)));
		}
		console.println("----------------");
		
	}

}
