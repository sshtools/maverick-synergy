package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

public class Ps extends ShellCommand {
	public Ps() {
		super("ps", SUBSYSTEM_SHELL, "[-a]", new Option("a", false, "Show processes run by other users"));
		setDescription("List all running processes");
		setBuiltIn(false);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		if(args.hasOption('a')) {
			Collection<VirtualProcess> rootProcesses = process.getProcessFactory().getRootProcesses();
			synchronized(rootProcesses) {
				for(VirtualProcess p : rootProcesses) {
					printProcess(process, p, 1);
				}
			}
		}
		else {
			VirtualProcess root = process.getRootProcess();
			printProcess(process, root, 1);
		}
	}
	
	void printProcess(VirtualProcess thisProcess, VirtualProcess processToPrint, int indent) throws IOException {
		Console console = thisProcess.getConsole();
		String commandName = processToPrint.getCommand().getCommandName();
		String fmt = "%" + indent + "s%-15d %" + ( Math.max(30 - commandName.length(), 10) ) + "s";
		console.printStringNewline(String.format(fmt, "", processToPrint.getPID(),commandName));
		for(VirtualProcess process : processToPrint.getChildren()) {
			printProcess(thisProcess, process, indent + 1);
		}
	}
}
