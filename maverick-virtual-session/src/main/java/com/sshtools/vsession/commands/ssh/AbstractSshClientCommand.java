package com.sshtools.vsession.commands.ssh;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.sshtools.client.SshClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CommandArgumentsParser;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public abstract class AbstractSshClientCommand extends ShellCommand {
	
	private String[] originalArguments = null;
	protected Options options = new Options();
	protected VirtualConsole console;
	
	public AbstractSshClientCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}
	
	public Options getOptions() {
		return options;
	}

	@Override
	public String getUsage() {
		StringWriter out = new StringWriter();

		PrintWriter pw = new PrintWriter(out);
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(pw, formatter.getWidth(), 
				"ssh",
				"", 
				getOptions(), 
				formatter.getLeftPadding(),
				formatter.getDescPadding(), "");
		pw.flush();

		String result = out.toString();

		return result;
	}
	
	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		this.console = console;
		
		String[] filteredArgs = filterArgs(args);
		CommandLine cli = CommandArgumentsParser.parse(getOptions(), filteredArgs, getUsage());
		
		SshClientArguments arguments = generateCommandArguments(cli, this.originalArguments);
		
		if (Log.isDebugEnabled()) {
			if(arguments.hasConnection()) {
				Log.debug("Using existing connection from {}", arguments.getConnection().getRemoteIPAddress());
			} else {
				Log.debug("The arguments parsed are {}", arguments);
			}
		}
		
		SshClient sshClient = null;
		try {
			
			sshClient = SshClientHelper.connectClient(arguments, console);
			
			runCommand(sshClient, arguments, console);
			
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			if (sshClient != null) {
				sshClient.close();
			}
		}
	}
	
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		if(line.wordIndex() > 0) {
			if(!line.word().startsWith("-")) {
				
					String hostname = getHostname(line);
					SshClientOptionsEvaluator.complete(hostname, candidates);
				
			}
		}
	}

	private String getHostname(ParsedLine line) {
		if(line.word().contains("@")) {
			return line.word().substring(line.word().indexOf('@')+1);
		}
		return line.word();
	}

	protected abstract void runCommand(SshClient sshClient, SshClientArguments arguments, VirtualConsole console);

	protected abstract SshClientArguments generateCommandArguments(CommandLine cli, String[] args) throws IOException, PermissionDeniedException;

	protected String[] filterArgs(String[] args) {
		this.originalArguments = args;
		int indexTillSshClientCommandFound = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(args);
		return Arrays.copyOfRange(args, 0, indexTillSshClientCommandFound + 1);
	}

}
