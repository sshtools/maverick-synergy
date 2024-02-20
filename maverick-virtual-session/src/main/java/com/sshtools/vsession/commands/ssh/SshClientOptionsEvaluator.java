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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.VirtualConsole;


public class SshClientOptionsEvaluator extends AbstractSshOptionsEvaluator {

	public static SshClientArguments evaluate(CommandLine commandLine, String[] originalArguments, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		if (Log.isDebugEnabled()) {
			Log.debug("The argument list passed as {}", commandLine.getArgList());
			
			List<String> optionList = Arrays.asList(commandLine.getOptions())
				.stream()
				.map(option -> String.format("%s -> %s", option.getArgName(), option.getValue()))
				.collect(Collectors.toList());
			
			Log.debug("The option list passed as {}", optionList);
		}
		
		SshClientArguments arguments = new SshClientArguments();
		
		parseCommand(originalArguments, arguments);
		parsePort(commandLine, arguments);
		parseLoginName(commandLine, arguments);
		parseIdentityFilename(commandLine, arguments, console);
		parseCiphers(commandLine, arguments);
		parseMacs(commandLine, arguments);
		parseSecurityLevel(commandLine, arguments);
		parseCompression(commandLine, arguments);
		
		parseDestination(commandLine, arguments);

		return arguments;
	}
	
	private static void parseCommand(String[] originalArguments, SshClientArguments arguments) {
		int indexTillSshClientCommandFound = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(originalArguments);
		if (originalArguments.length == (indexTillSshClientCommandFound + 1)) {
			// no execute command found
			return;
		}
		
		String[] executeCommand = Arrays.copyOfRange(originalArguments, indexTillSshClientCommandFound + 1, originalArguments.length);
		String command = String.join(" ", executeCommand);
		arguments.setCommand(command);
	}
	
}
