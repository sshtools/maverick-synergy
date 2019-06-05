package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.Command;
import com.sshtools.server.vshell.Msh;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

/**
 * Usage: exit
 * @author lee
 *
 */
public class Exit extends ShellCommand {

	public Exit() {
		super("exit", ShellCommand.SUBSYSTEM_SHELL);
		setDescription("Exits the current shell");
		setBuiltIn(true);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		Command parentCommand = process.getCommand();
		if(parentCommand instanceof Msh) {
			((Msh)parentCommand).exit();
		}
	
		
	}
}
