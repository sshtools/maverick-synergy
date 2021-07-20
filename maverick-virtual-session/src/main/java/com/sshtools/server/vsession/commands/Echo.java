
package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: echo [-n] [string]
 * @author lee
 *
 */
public class Echo extends ShellCommand {

	public Echo() {
		super("echo", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("echo [options] <string>",
				"-n       Don't print newline"), "Echo a message to the screen");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		StringBuilder bui = new StringBuilder();

		for (int i = 1 ; i < args.length; i++) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			bui.append(args[i]);
		}
		if (CliHelper.hasShortOption(args, 'n')) {
			console.print(bui.toString());
		} else {
			console.println(bui.toString());
		}
	}
}
