/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.vsession.commands.ssh;

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
				Log.debug("Using existing connection from {}", arguments.getConnection().getRemoteAddress());
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
