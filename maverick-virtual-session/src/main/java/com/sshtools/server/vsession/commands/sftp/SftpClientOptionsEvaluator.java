package com.sshtools.server.vsession.commands.sftp;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.logger.Log;
import com.sshtools.server.vsession.commands.sftp.SftpClientOptions.Port;
import com.sshtools.vsession.commands.ssh.CommandUtil;

public class SftpClientOptionsEvaluator {
	
	public static String[] extractUserAndDestination(List<String> arguments) {
		
		if (arguments.size() != 2) {
			throw new IllegalArgumentException("Expected sftp command is of type `sftp user@host`.");
		}
		
		String userWithDestination = arguments.get(1);
		if (!userWithDestination.contains("@")) {
			throw new IllegalArgumentException("Expected sftp destination is as `user@host`.");
		}
		
		String[] parts = userWithDestination.split("@");
		String user = parts[0];
		String destination = parts[1];
		
		if (CommandUtil.isEmpty(user)) {
			throw new IllegalArgumentException("User cannot be empty.");
		}
		
		if (CommandUtil.isEmpty(destination)) {
			throw new IllegalArgumentException("Destination cannot be empty");
		}
		
		return new String[] {user, destination};
		
	}

	public static SftpClientArguments evaluate(CommandLine commandLine) {

		if (Log.isDebugEnabled()) {
			Log.debug("The argument list passed as %s", commandLine.getArgList());

			List<String> optionList = Arrays.asList(commandLine.getOptions()).stream()
					.map(option -> String.format("%s -> %s", option.getArgName(), option.getValue()))
					.collect(Collectors.toList());

			Log.debug("The option list passed as %s", optionList);
		}

		SftpClientArguments arguments = new SftpClientArguments();

		parsePort(commandLine, arguments);

		return arguments;
	}

	private static void parsePort(CommandLine commandLine, SftpClientArguments arguments) {
		int port = 22;

		if (commandLine.hasOption(Port.PORT_OPTION)) {
			String portValue = commandLine.getOptionValue(Port.PORT_OPTION);
			try {
				port = Integer.parseInt(portValue);
			} catch (Exception e) {
				port = 22;
			}
		}

		arguments.setPort(port);
	}
}
