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
