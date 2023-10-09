package com.sshtools.vsession.commands.ssh;

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
