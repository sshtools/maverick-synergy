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
package com.sshtools.server.vsession.commands.os;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualSessionPolicy;

public class Shell extends AbstractOSCommand {

	public Shell() {
		super("osshell", ShellCommand.SUBSYSTEM_SYSTEM, "osshell", "Run a native shell");
		setDescription("The current operating systems shell");
		setBuiltIn(false);
	}

	protected List<String> configureCommand(String cmd, List<String> cmdArgs, VirtualConsole console) throws IOException {
		
		List<String> args = new ArrayList<>();
		String shellCommand = console.getContext().getPolicy(VirtualSessionPolicy.class).getShellCommand();
		if (SystemUtils.IS_OS_WINDOWS) {
			if(StringUtils.isBlank(shellCommand)) {
				args.add("C:\\Windows\\System32\\cmd.exe");
			} else {
				args.add(shellCommand);
				args.addAll(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellArguments());
			}
		}
		else {
			
			if(SystemUtils.IS_OS_MAC_OSX) {
				if(StringUtils.isBlank(shellCommand)) {
					shellCommand = findCommand("zsh", "/bin/zsh", "bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh");
					if(shellCommand == null)
						throw new IOException("Cannot find OSX shell.");
				}
			} else {
				if(StringUtils.isBlank(shellCommand)) {
					shellCommand = findCommand("bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh");
					if(shellCommand == null)
						throw new IOException("Cannot find shell.");
				}
			}
		
			args.add(shellCommand);
			args.addAll(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellArguments());
		}
		
		return args;
	}

}