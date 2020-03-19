package com.sshtools.vsession.commands.ssh;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.logger.Log;
import com.sshtools.vsession.commands.ssh.SshClientOptions.LoginName;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Port;


public class SshClientOptionsEvaluator {

	public static SshClientArguments evaluate(CommandLine commandLine) {
		
		if (Log.isDebugEnabled()) {
			Log.debug("The argument list passed as %s", commandLine.getArgList());
			
			List<String> optionList = Arrays.asList(commandLine.getOptions())
				.stream()
				.map(option -> String.format("%s -> %s", option.getArgName(), option.getValue()))
				.collect(Collectors.toList());
			
			Log.debug("The option list passed as %s", optionList);
		}
		
		SshClientArguments arguments = new SshClientArguments();
		
		parseDestination(commandLine, arguments);
		parsePort(commandLine, arguments);
		parseLoginName(commandLine, arguments);

		return arguments;
	}
	
	private static void parseDestination(CommandLine commandLine, SshClientArguments arguments) {
		// [ssh, admin@localhost]
		List<String> commandLineArguments = commandLine.getArgList();
		String destination = commandLineArguments.get(1);
		String loginName = null;
		if (destination.contains("@")) {
			String[] destinationParts = destination.split("@");
			loginName = destinationParts[0];
			destination = destinationParts[1];
		}
	
		arguments.setDestination(destination);
		arguments.setLoginName(loginName);
	}
	
	private static void parsePort(CommandLine commandLine, SshClientArguments arguments) {
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
	
	private static String parseLoginName(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(LoginName.LOGIN_NAME_OPTION)) {
			arguments.setLoginName(commandLine.getOptionValue(LoginName.LOGIN_NAME_OPTION));
		}
		
		return null;
	}
	
}
