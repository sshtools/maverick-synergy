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
package com.sshtools.server.vshell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.server.vshell.CmdLine.Condition;
import com.sshtools.server.vshell.commands.Alias;
import com.sshtools.server.vshell.terminal.Console;


public class Msh extends ShellCommand {
	private Throwable lastError;

	private boolean exit;
	private String prompt;
	protected CommandFactory<ShellCommand> commandFactory;

	protected Map<Integer, Job> runningJobs = new HashMap<Integer, Job>();
	private int nextJobId = 1;
	public static final String LOGIN_CONTEXT = "loginContext";

	public Msh(CommandFactory<ShellCommand> commandFactory) {
		super("msh", SUBSYSTEM_SHELL, "[<script>]", new Option("c",
				"Execute arguments as a command"), new Option("s",
				"Process commands from standard input"));
		setDescription("Maverick SSHD shell");
		setBuiltIn(false);
		this.commandFactory = commandFactory;
	}

	public Msh(String name, String subsystem, String signature,
			CommandFactory<ShellCommand> commandFactory, Option... options) {
		super(name, subsystem, signature, options);
		this.commandFactory = commandFactory;
	}

	public Msh(String name, String subsystem, String signature,
			CommandFactory<ShellCommand> commandFactory) {
		super(name, subsystem, signature);
		this.commandFactory = commandFactory;
	}

	public Msh(String name, String subsystem,
			CommandFactory<ShellCommand> commandFactory) {
		super(name, subsystem);
		this.commandFactory = commandFactory;
	}

	protected void setCommandFactory(CommandFactory<ShellCommand> commandFactory) {
		this.commandFactory = commandFactory;
	}

	public String expandEnvironmentVariables(String value, Map<String,String> additionalReplacements) {
		
		Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(value);

		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			String attributeName = matcher.group(1);
			String replacement;
			if(!process.getEnvironment().containsKey(attributeName) && !additionalReplacements.containsKey(attributeName)) {
				Log.debug("Replacement token " + attributeName + " not in list to replace from");
				continue;
			}
		    replacement = process.getEnvironment().containsKey(attributeName) 
		    		? process.getEnvironment().get(attributeName).toString() 
		    				: additionalReplacements.get(attributeName);
		    builder.append(value.substring(i, matcher.start()));
		    if (replacement == null) {
		        builder.append(matcher.group(0));
		    } else {
		        builder.append(replacement);
		    }
		    i = matcher.end();
		}
		
	    builder.append(value.substring(i, value.length()));
		
		return builder.toString();
	}

	public ShellPolicy getShellPolicy() {
		return getProcess().getContext().getPolicy(ShellPolicy.class);
	}
	
	protected void runShell(VirtualProcess process) throws IOException {
		Console reader = process.getConsole();
		
		Map<String,String> additionalReplacements = new HashMap<String,String>();
		while (!exit) {
			prompt = process.getEnvironment().getOrDefault("PROMPT", "# ").toString();
			prompt = expandEnvironmentVariables(prompt, additionalReplacements);
			try {
				String line = reader.readLine(prompt);
				if(Log.isDebugEnabled()) {
					Log.debug("Received: " + line);
				}
				if (line == null) {
					exit = true;
				} else {
					parseLine(process, line);
				}
				reader.getCursorBuffer().clearBuffer();
			} catch (InterruptedIOException ie) {
				reader.printNewline();
			}
		}
		
		if(Log.isDebugEnabled()) {
			Log.debug("Exiting shell " + process.hashCode());
		}
	}

	public void run(CommandLine cli, VirtualProcess process)
			throws IOException, PermissionDeniedException {
		Console reader = process.getConsole();
		String[] args = cli.getArgs();
		if (args.length == 1) {
			runShell(process);
		} else {
			if (cli.hasOption("c")) {
				// Execute the following arguments as a command
				List<String> commandArgs = new ArrayList<String>();
				for (int i = 1; i < args.length; i++) {
					commandArgs.add(args[i]);
				}
				parseArgs(process, commandArgs);
			} else {
				source(process,
						process.getCurrentDirectory().resolveFile(args[1]));
			}
			if (cli.hasOption("s")) {
				// Read commands from standard input
				try {
					String line = reader.readLine();
					if (line == null) {
						exit = true;
					} else {
						parseLine(process, line);
					}
					reader.getCursorBuffer().clearBuffer();
				} catch (InterruptedIOException ie) {
					reader.printNewline();
				}
			}

		}
	}

	protected void source(VirtualProcess process, AbstractFile file)
			throws IOException {
		InputStream inputStream = file.getInputStream();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(
					inputStream));
			String line = null;
			while ((line = r.readLine()) != null) {
				parseLine(process, line);
			}
		} finally {
			inputStream.close();
		}
	}

	protected void source(VirtualProcess process, InputStream in)
			throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		try {
			String line = null;
			while ((line = r.readLine()) != null) {
				parseLine(process, line);
			}
		} finally {
			r.close();
		}
	}

	protected void parseArgs(VirtualProcess process, List<String> lineArgs)
			throws IOException {
		Console reader = process.getConsole();
		SessionChannelServer session = process.getSessionChannel();
		LineParser lineParser = new LineParser(process.getEnvironment());

		if (lineArgs.size() > 0 && !lineArgs.get(0).startsWith("#")) {
			// Expand any alias commands
			expandAliases(session, lineParser, lineArgs, exitCode);
			exitCode = spawn(reader, process, lineArgs.toArray(new String[0]), false);
		}

	}

	protected void parseLine(VirtualProcess process, String line)
			throws IOException {
		Console reader = process.getConsole();
		SessionChannelServer session = process.getSessionChannel();
		LineParser lineParser = new LineParser(process.getEnvironment());
		line = line.trim();

		if (!line.startsWith("#") && !line.equals("")) {
			CmdLine lastCommand = null;
			boolean exit = false;

			for (CmdLine l : lineParser.parseCommands(line, exitCode)) {

				if (exit)
					break;

				switch (lastCommand == null ? 0 : lastCommand.getExitCode()) {
				case 0:
				case STILL_ACTIVE:
					if (lastCommand != null
							&& lastCommand.getCondition() == Condition.ExecNextCommandOnFailure) {
						exit = true;
						continue;
					}
					break;
				default:
					if (lastCommand != null
							&& lastCommand.getCondition() == Condition.ExecNextCommandOnSuccess) {
						exit = true;
						continue;
					}
					break;
				}
				lastCommand = l;
				
				
				expandAliases(session, lineParser, l.getArgs(), exitCode);
				
				l.setExitCode(exitCode = spawn(process.getConsole(), process,
						l.getArgArray(), l.isBackground()));
				reader.getHistory().addToHistory(line);
			}

		}

	}

	private void expandAliases(SessionChannelServer session, LineParser lineParser,
			List<String> lineArgs, int lastExitCode) {
		if (lineArgs.size() > 0) {
			String cmd = lineArgs.get(0);
			if (Alias.hasAlias(cmd, session.getConnection().getUsername())) {
				 lineArgs.remove(0);
				 lineArgs.addAll(0,
				 lineParser.parse(Alias.getAliasCommand(cmd, session.getConnection().getUsername()), lastExitCode));
			}
		}
	}

	protected int spawn(final Console term, final VirtualProcess process,
			final String[] args, boolean background) throws IOException {

		try {
			return doSpawn(term, process, args, background);
		} catch (PermissionDeniedException pde) {
			lastError = pde;
			if(Log.isInfoEnabled())
				Log.info("Failed to create ShellCommand instance for "
						+ args[0], pde);
			term.printNewline();
			term.printStringNewline("You are not allowed to run '" + args[0]
					+ "'.");
			term.printStringNewline(pde.getMessage());
			term.printNewline();
			return 98;
		} catch (UsageException ex) {
			term.printStringNewline(args[0] + ": usage: " + ex.getMessage());
			return 1;
		} catch (ParseException ie) {
			lastError = ie;
			if(Log.isInfoEnabled())
				Log.info("Failed to parse command line for " + args[0], ie);
			term.printStringNewline("The command was recognized but could not run");
			return 1;
		} catch (Throwable t) {
			if(t.getCause()!=null) {
				lastError = t.getCause();
			} else {
				lastError = t;
			}
			Log.error("Failed to run command line " + args[0], t);
			term.printStringNewline(lastError.getMessage() == null 
					? lastError.getClass().getName() : lastError.getMessage());
			return 99;
		}
	}

	protected int doSpawn(final Console term, final VirtualProcess process,
			final String[] args, final boolean background)
			throws UnsupportedCommandException, IllegalAccessException,
			InstantiationException, ParseException, IOException,
			PermissionDeniedException, UsageException {

		final ShellCommand cmd;

		if (args[0].equals("sh") || args[0].equals("msh")) {
			cmd = new Msh(commandFactory);
		} else {
			cmd = commandFactory
					.createCommand(args[0], process.getConnection());
		}

		if (process.getMsh().getShellPolicy()
				.checkPermission(process.getConnection(),
						ShellPolicy.EXEC, cmd.getCommandName())) {
			if(Log.isDebugEnabled()) {
				Log.debug(String.format("Executing command %s", StringUtils.join(args, " ")));
			}
			return runCommandWithArgs(args, cmd, process, background);
		} else {
			if(Log.isDebugEnabled()) {
				Log.debug(String.format("Cannot execute %s", StringUtils.join(args, " ")));
			}
			throw new SecurityException(
					"You are not allowed to run the command "
							+ cmd.getCommandName() + ".");
		}
	}

	private int runCommandWithArgs(String[] args, ShellCommand cmd,
			VirtualProcess process, boolean background) throws ParseException,
			IOException, PermissionDeniedException, UsageException {

		if (!process
				.getMsh()
				.getShellPolicy()
				.checkPermission(process.getConnection(),
						ShellPolicy.EXEC, cmd.getCommandName())) {
			throw new PermissionDeniedException(
					"Permission denied. Cannot execute " + cmd.getCommandName());
		}

		String name = args[0];
		args = (String[])ArrayUtils.remove(args, 0);
		
		DefaultParser parser = new DefaultParser();
		Options options = cmd.getOptions();
		if (options == null) {
			options = new Options();
		}
		CommandLine cli;
		try {
			cli = parser.parse(options, args, !cmd.hasFixedOptions());
		} catch (ParseException e) {
			throw new UsageException(cmd.getSignature());
		}

		cli.getArgList().add(0, name);
		
		VirtualProcess newProcess;

		if (cmd.isBuiltIn() && !background) {
			newProcess = process;
		} else {
			newProcess = process.getProcessFactory().createChildProcess(
					process, cmd, this);
		}

		cmd.init(newProcess);

		if (background) {

			if (runningJobs.size() == 0)
				nextJobId = 1;

			Job job = new Job(nextJobId++, cmd, cli, newProcess);
			runningJobs.put(job.getJobId(), job);

			process.getConsole().printStringNewline(
					"[" + job.getJobId() + "] "
							+ String.valueOf(newProcess.hashCode()));

			job.start();

			return 0;
		} else {
			try {
				cmd.run(cli, newProcess);
				return cmd.getExitCode();
			} finally {
				if (!cmd.isBuiltIn()) {
					newProcess.destroy();
				}
			}
		}
	}

	public Throwable getLastError() {
		return lastError;
	}

	public void exit() {
		exit = true;
	}

	public CommandFactory<ShellCommand> getCommandFactory() {
		return commandFactory;
	}

	class Job extends Thread {
		int id;
		Command cmd;
		CommandLine cli;
		VirtualProcess process;
		boolean running = true;
		Throwable lastError;

		Job(int id, Command cmd, CommandLine cli, VirtualProcess process) {
			this.id = id;
			this.cmd = cmd;
			this.cli = cli;
			this.process = process;
		}

		public void run() {

			try {
				cmd.run(cli, process);
			} catch (Throwable t) {
				lastError = t;
			} finally {
				if (!cmd.isBuiltIn()) {
					process.destroy();
				}
			}

			running = false;

			// process.getConsole().printStringNewline("[" + getJobId() +
			// "]   Done         " + cmd.getCommandName());

			runningJobs.remove(id);
		}

		boolean isRunning() {
			return running;
		}

		int getJobId() {
			return id;
		}

		int getExitCode() {
			return cmd.getExitCode();
		}
	}
}
