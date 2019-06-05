package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Date extends ShellCommand {

	public Date() {
		super("date", ShellCommand.SUBSYSTEM_SHELL, "<text>", new Option("t", false, "Just output time"), new Option("d", false,
			"Just output date"), new Option("l", false, "Use long format"), new Option("f", true, "Custom format"));
		setDescription("Display the current date");
		setBuiltIn(true);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		DateFormat fmt = args.hasOption('l') ? DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG) : DateFormat
			.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		if (args.hasOption('t')) {
			if (args.hasOption('l')) {
				fmt = DateFormat.getTimeInstance(DateFormat.LONG);
			} else {
				fmt = DateFormat.getTimeInstance(DateFormat.SHORT);
			}
		} else if (args.hasOption('d')) {
			if (args.hasOption('l')) {
				fmt = DateFormat.getDateInstance(DateFormat.LONG);
			} else {
				fmt = DateFormat.getDateInstance(DateFormat.SHORT);
			}
		} else if (args.hasOption('f')) {
			fmt = new SimpleDateFormat(args.getOptionValue('f'));
		}
		process.getConsole().printStringNewline(fmt.format(new java.util.Date()));
	}
}
