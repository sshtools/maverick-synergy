package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.server.vshell.Command;
import com.sshtools.server.vshell.ConsoleOutputStream;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.ShellUtilities;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

public class Help<T extends AbstractFile> extends ShellCommand {

	public Help() {
		super("help", SUBSYSTEM_HELP, "[<command>]");
		setDescription("Display information about the available commands.");
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException {
		java.util.Set<String> cmds = process.getMsh().getCommandFactory().getSupportedCommands();
		Console term = process.getConsole();

		String[] args = cli.getArgs();
		if (args.length == 2 && cmds.contains(args[1])) {
			process.getMsh().getShellPolicy().checkPermission(
					process.getConnection(), ShellPolicy.EXEC, args[1]);
			try {

				Command cmd = process.getMsh().getCommandFactory().createCommand(args[1], process.getConnection());

				HelpFormatter hf = new HelpFormatter();
				Options options = cmd.getOptions();
				PrintWriter pw = new PrintWriter(new ConsoleOutputStream(term), true);
				if (options.getOptions().size() == 0) {
					hf.printUsage(pw, term.getTermwidth(), cmd.getCommandName() + " " + cmd.getSignature(), options);
				} else {
					hf.printUsage(pw, term.getTermwidth(), cmd.getCommandName(), options);
					hf.printWrapped(pw, term.getTermwidth(), "    " + cmd.getSignature() + "\n");
				}
				if (options != null && options.getOptions().size() != 0) {
					hf.printWrapped(pw, term.getTermwidth(), "Options for " + cmd.getCommandName() + ":-");
					hf.printOptions(pw, term.getTermwidth(), options, 4, 8);
				}
			} catch (Exception e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		} else {
			// Create a list of subsystems and their commands
			HashMap<String, Map<String, Command>> subsystems = new HashMap<String, Map<String, Command>>();
			Iterator<String> it = cmds.iterator();
			Command cmd;
			Map<String, Command> comandMap;

			while (it.hasNext()) {
				try {
					String cmdName = (String) it.next();

					if (process.getMsh().getShellPolicy()
						.checkPermission(process.getConnection(), ShellPolicy.EXEC, cmdName)) {
						cmd = process.getMsh().getCommandFactory().createCommand(cmdName, process.getConnection());

						if(!cmd.isHidden()) {
							if (!subsystems.containsKey(cmd.getSubsystem())) {
								comandMap = new HashMap<String, Command>();
								comandMap.put(cmd.getCommandName(), cmd);
								subsystems.put(cmd.getSubsystem(), comandMap);
							} else {
								comandMap = subsystems.get(cmd.getSubsystem());
								comandMap.put(cmd.getCommandName(), cmd);
							}
						}
					}
				} catch (Exception e) {
				}
			}

			term.printNewline();
			term.printStringNewline("The following commands are available:");
			term.printNewline();
			Iterator<Map.Entry<String, Map<String, Command>>> subsystemsIterator = subsystems.entrySet().iterator();
			Map.Entry<String, Map<String, Command>> entry;
			while (subsystemsIterator.hasNext()) {
				entry = subsystemsIterator.next();
				term.printStringNewline((String) entry.getKey() + " commands:");
				comandMap = entry.getValue();
				for (Command shellCmd : comandMap.values()) {
					term.printStringNewline(ShellUtilities.padString("", 5)
						+ ShellUtilities.padString(shellCmd.getCommandName(), 15) + shellCmd.getDescription());
				}

				term.printNewline();
			}
			term.printStringNewline(ShellUtilities.padString("", 5) + ShellUtilities.padString("help [command]", 15)
				+ "Display command signature.");
		}
	}
}
