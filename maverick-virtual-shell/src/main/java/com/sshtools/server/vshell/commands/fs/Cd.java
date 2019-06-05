package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.commands.AbstractFileCommand;

/**
 * Usage: cd [directory]
 * @author lee
 *
 */
public class Cd extends AbstractFileCommand {
	public Cd() {
		super("cd", ShellCommand.SUBSYSTEM_FILESYSTEM, "cd [directory]");
		setDescription("Moves the working directory to a new directory");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException, PermissionDeniedException {
		String[] args = cli.getArgs();
		if (args.length > 2)
			throw new IllegalArgumentException("Too many arguments.");
		if (args.length > 1) {
			process.setCurrentDirectory(args[1]);
		} else {
			try {
				process.setCurrentDirectory(process.getEnvironment().getOrDefault("HOME", "").toString());
			} catch (PermissionDeniedException e) {
				throw new IllegalAccessError();
			}
		}
	}
}
