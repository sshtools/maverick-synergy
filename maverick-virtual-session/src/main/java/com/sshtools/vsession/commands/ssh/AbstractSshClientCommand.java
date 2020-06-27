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
package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

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
			Log.debug(String.format("The arguments parsed are %s", arguments));
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

	protected abstract void runCommand(SshClient sshClient, SshClientArguments arguments, VirtualConsole console);

	protected abstract SshClientArguments generateCommandArguments(CommandLine cli, String[] args) throws IOException, PermissionDeniedException;

	protected String[] filterArgs(String[] args) {
		this.originalArguments = args;
		int indexTillSshClientCommandFound = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(args);
		return Arrays.copyOfRange(args, 0, indexTillSshClientCommandFound + 1);
	}

}
