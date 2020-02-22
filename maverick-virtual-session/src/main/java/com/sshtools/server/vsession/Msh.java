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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.ParseException;
import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.util.Utils;
import com.sshtools.server.vsession.CmdLine.Condition;
import com.sshtools.server.vsession.commands.Alias;


public class Msh extends ShellCommand {
	private Throwable lastError;

	private boolean exit;
	private String prompt;
	protected CommandFactory<ShellCommand> commandFactory;

	protected Map<Long, Job> runningJobs = new HashMap<>();
	private long nextJobId = 1;
	public static final String LOGIN_CONTEXT = "loginContext";

	private List<MshListener> listeners = new ArrayList<>();
	
	public Msh(CommandFactory<ShellCommand> commandFactory) {
		super("msh", SUBSYSTEM_SHELL, "Usage: msh [script]", "A basic interactive shell for executing commands.");
		setBuiltIn(false);
		this.commandFactory = commandFactory;
	}
	
	public Msh(String name, String subsystem, String usage, String description) {
		super(name, subsystem, usage, description);
		setBuiltIn(false);
	}

	public void addListener(MshListener listener) {
		listeners.add(listener);
	}
	
	protected void setCommandFactory(CommandFactory<ShellCommand> commandFactory) {
		this.commandFactory = commandFactory;
	}

	public String expandEnvironmentVariables(Environment env, String value, Map<String,String> additionalReplacements) {
		
		if(Objects.isNull(value)) {
			return value;
		}
		Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(value);

		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			String attributeName = matcher.group(1);
			String replacement;
			if(!env.containsKey(attributeName) && !additionalReplacements.containsKey(attributeName)) {
				Log.debug("Replacement token " + attributeName + " not in list to replace from");
				continue;
			}
		    replacement = env.containsKey(attributeName) 
		    		? env.get(attributeName).toString() 
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
	
	protected void runShell(VirtualConsole console) throws IOException {
		
		Map<String,String> additionalReplacements = new HashMap<String,String>();
		if(!console.getEnvironment().containsKey("PROMPT")) {
			console.getEnvironment().put("PROMPT", "# ");
		}
		while (!exit) {
			prompt = (String) console.getEnvironment().get("PROMPT");
			
			prompt = expandEnvironmentVariables(console.getEnvironment(), prompt, additionalReplacements);
			try {
				String line = console.readLine(prompt);
				if(Log.isDebugEnabled()) {
					Log.debug("Received: " + line);
				}
				if (line == null) {
					exit = true;
				} else {
					parseLine(line, console);
				}
			} catch (InterruptedIOException ie) {
				console.println();
			} catch(EndOfFileException eofe) {
				exit = true;
			}
		}
		
		if(Log.isDebugEnabled()) {
			Log.debug("Exiting shell");
		}
	}

	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException {

		if (args.length <= 1) {
			runShell(console);
		} else {
			if ("c".equals(args[1]) && args.length >=3) {
				// Execute the following arguments as a command
				List<String> commandArgs = Arrays.asList(args).subList(1, args.length);
				parseArgs(console, commandArgs);
			} else {
				source(console,
						console.getCurrentDirectory().resolveFile(args[2]));
			}
			if ("s".equals(args[2]) && args.length >=3) {
				// Read commands from standard input
				try {
					String line = console.readLine();
					if (line == null) {
						exit = true;
					} else {
						parseLine(line, console);
					}
					console.clear();
				} catch (InterruptedIOException ie) {
					console.println();
				}
			}

		}
	}

	protected void source(VirtualConsole console, AbstractFile file)
			throws IOException {
		InputStream inputStream = file.getInputStream();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(
					inputStream));
			String line = null;
			while ((line = r.readLine()) != null) {
				parseLine(line, console);
			}
		} finally {
			inputStream.close();
		}
	}

	protected void source(VirtualConsole console, InputStream in)
			throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		try {
			String line = null;
			while ((line = r.readLine()) != null) {
				parseLine(line, console);
			}
		} finally {
			r.close();
		}
	}

	protected void parseArgs(VirtualConsole console, List<String> lineArgs)
			throws IOException {
		
		LineParser lineParser = new LineParser(console.getEnvironment());
		if (lineArgs.size() > 0 && !lineArgs.get(0).startsWith("#")) {
			expandAliases(console, lineParser, lineArgs, exitCode);
			exitCode = spawn(console, lineArgs.toArray(new String[0]), false);
		}

	}

	protected void parseLine(String line, VirtualConsole console)
			throws IOException {

		LineParser lineParser = new LineParser(console.getEnvironment());
		line = line.trim();

		if (!line.startsWith("#") && !line.equals("")) {
			CmdLine lastCommand = null;
			boolean exit = false;

			console.getLineReader().getVariables().put(LineReader.DISABLE_HISTORY, Boolean.TRUE);
			
			try {
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
					
					
					expandAliases(console, lineParser, l.getArgs(), exitCode);
					
					l.setExitCode(exitCode = spawn(console, l.getArgArray(), l.isBackground()));
				}
			
			} finally {
				console.getLineReader().getVariables().remove(LineReader.DISABLE_HISTORY);
			}

		}

	}

	private void expandAliases(VirtualConsole console, LineParser lineParser,
			List<String> lineArgs, int lastExitCode) {
		if (lineArgs.size() > 0) {
			String cmd = lineArgs.get(0);
			if (Alias.hasAlias(cmd, console.getConnection().getUsername())) {
				 lineArgs.remove(0);
				 lineArgs.addAll(0,
				 lineParser.parse(Alias.getAliasCommand(cmd, console.getConnection().getUsername()), lastExitCode));
			}
		}
	}

	protected int spawn(final VirtualConsole console,
			final String[] args, boolean background) throws IOException {

		try {
			return doSpawn(console, args, background);
		} catch (PermissionDeniedException pde) {
			lastError = pde;
			if(Log.isInfoEnabled())
				Log.info("Failed to create ShellCommand instance for "
						+ args[0], pde);
			console.println();
			console.println("You are not allowed to run '" + args[0]
					+ "'.");
			console.println(pde.getMessage());
			console.println();
			return 98;
		} catch (UsageException ex) {
			console.println(args[0] + ": usage: " + ex.getMessage());
			return 1;
		} catch (ParseException ie) {
			lastError = ie;
			if(Log.isInfoEnabled())
				Log.info("Failed to parse command line for " + args[0], ie);
			console.println("The command was recognized but could not run");
			return 1;
		} catch (Throwable t) {
			if(t.getCause()!=null) {
				lastError = t.getCause();
			} else {
				lastError = t;
			}
			Log.error("Failed to run command line " + args[0], t);
			console.println();
			console.println(lastError.getMessage() == null 
					? lastError.getClass().getName() : lastError.getMessage());
			return 99;
		}
	}

	protected int doSpawn(
			VirtualConsole console,
			String[] args, 
			boolean background)
			throws UnsupportedCommandException, IllegalAccessException,
			InstantiationException, ParseException, IOException,
			PermissionDeniedException, UsageException {

		final ShellCommand cmd;

		if (args[0].equals("sh") || args[0].equals("msh")) {
			cmd = new Msh(commandFactory);
		} else {
			cmd = commandFactory
					.createCommand(args[0], console.getConnection());
		}

		if (console.getConnection().getContext().getPolicy(ShellPolicy.class)
				.checkPermission(console.getConnection(),
						ShellPolicy.EXEC, cmd.getCommandName())) {
			if(Log.isDebugEnabled()) {
				Log.debug(String.format("Executing command %s", Utils.join(args, " ")));
			}
			return runCommandWithArgs(args, cmd, console, background);
		} else {
			if(Log.isDebugEnabled()) {
				Log.debug(String.format("Cannot execute %s", Utils.join(args, " ")));
			}
			throw new SecurityException(
					"You are not allowed to run the command "
							+ cmd.getCommandName() + ".");
		}
	}

	private int runCommandWithArgs(String[] args, ShellCommand cmd,
			VirtualConsole console, boolean background) throws ParseException,
			IOException, PermissionDeniedException, UsageException {

		if (!console.getContext().getPolicy(ShellPolicy.class)
				.checkPermission(console.getConnection(),
						ShellPolicy.EXEC, cmd.getCommandName())) {
			throw new PermissionDeniedException(
					"Permission denied. Cannot execute " + cmd.getCommandName());
		}
	
		if (background) {

			if (runningJobs.size() == 0)
				nextJobId = 1;

			Job job = new Job(nextJobId++, cmd, args, console);
			runningJobs.put(job.getJobId(), job);

			console.println("[" + job.getJobId() + "] ");

			job.start();

			return 0;
		} else {
			for(MshListener listener : listeners) {
				listener.commandStarted(cmd, args, console);
			}
			try {
				cmd.run(args, console);
			} catch(UsageException e) {
				console.println();
				console.println(cmd.getUsage());
			} finally {
				for(MshListener listener : listeners) {
					listener.commandFinished(cmd, args, console);
				}
			}
			return cmd.getExitCode();
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
		long id;
		Command cmd;
		boolean running = true;
		String[] args;
		VirtualConsole console;
		Throwable lastError;

		Job(long id, Command cmd, String[] args, VirtualConsole console) {
			this.id = id;
			this.cmd = cmd;
			this.args = args;
			this.console = console;
		}

		public void run() {

			for(MshListener listener : listeners) {
				listener.commandStarted(cmd, args, console);
			}
			try {
				cmd.run(args, console);
			} catch (Throwable t) {
				lastError = t;
			} finally {
				for(MshListener listener : listeners) {
					listener.commandFinished(cmd, args, console);
				}
			}

			running = false;
			runningJobs.remove(id);
		}

		boolean isRunning() {
			return running;
		}

		long getJobId() {
			return id;
		}

		int getExitCode() {
			return cmd.getExitCode();
		}
	}

	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		
	}

	public void killProcess(long pid) {
		
		if(runningJobs.containsKey(pid)) {
			runningJobs.get(pid).interrupt();
		}
	}
}
