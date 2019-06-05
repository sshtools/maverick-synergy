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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.scp.ScpCommand;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Scp extends ShellCommand {

	public Scp() {
		super("scp", ShellCommand.SUBSYSTEM_FILESYSTEM, "", new Option("t",
				false, "to"), new Option("f", false, "from"), new Option("r",
				false, "recurse"), new Option("v", false, "verbose"),
				new Option("d", false, "directory"));
		setDescription("Secure Copy");
	}

	VirtualProcess process;

	public boolean isHidden() {
		return true;
	}
	
	public void run(CommandLine args, VirtualProcess process)
			throws IOException, PermissionDeniedException {

		this.process = process;

		ScpCommand cmd = new ScpCommand() {
			public void onStart() { };

		};
		
		cmd.init(process.getSessionChannel());
		Map<String,String> env = new HashMap<String,String>();
		process.getEnvironment().putAll(env);
		StringBuffer cmdline = new StringBuffer();
		for(String arg : args.getArgList()) {
			if(cmdline.length() > 0) {
				cmdline.append(' ');
			}
			cmdline.append(arg);
		}
		cmd.createProcess(args.getArgs(), env);
		cmd.start();
		cmd.run();
	}

}
