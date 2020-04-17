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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.logger.Log;
import com.sshtools.vsession.commands.ssh.SshClientOptions.CipherSpec;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Compression;
import com.sshtools.vsession.commands.ssh.SshClientOptions.IdentityFile;
import com.sshtools.vsession.commands.ssh.SshClientOptions.LoginName;
import com.sshtools.vsession.commands.ssh.SshClientOptions.MacSpec;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Port;
import com.sshtools.vsession.commands.ssh.SshClientOptions.SecurityLevel;


public class SshClientOptionsEvaluator {

	public static SshClientArguments evaluate(CommandLine commandLine, String[] originalArguments) {
		
		if (Log.isDebugEnabled()) {
			Log.debug("The argument list passed as %s", commandLine.getArgList());
			
			List<String> optionList = Arrays.asList(commandLine.getOptions())
				.stream()
				.map(option -> String.format("%s -> %s", option.getArgName(), option.getValue()))
				.collect(Collectors.toList());
			
			Log.debug("The option list passed as %s", optionList);
		}
		
		SshClientArguments arguments = new SshClientArguments();
		
		parseCommand(originalArguments, arguments);
		parseDestination(commandLine, arguments);
		parsePort(commandLine, arguments);
		parseLoginName(commandLine, arguments);
		parseIdentityFilename(commandLine, arguments);
		parseCiphers(commandLine, arguments);
		parseMacs(commandLine, arguments);
		parseSecurityLevel(commandLine, arguments);
		parseCompression(commandLine, arguments);

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
	
	private static void parseLoginName(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(LoginName.LOGIN_NAME_OPTION)) {
			arguments.setLoginName(commandLine.getOptionValue(LoginName.LOGIN_NAME_OPTION));
		}
		
	}
	
	private static void parseIdentityFilename(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(IdentityFile.IDENTITY_FILE_OPTION)) {
			arguments.setIdentityFile(commandLine.getOptionValue(IdentityFile.IDENTITY_FILE_OPTION));
		}
		
	}
	
	private static void parseCiphers(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(CipherSpec.CIPHER_SPEC_OPTION)) {
			String[] cipherSpecParts = commandLine.getOptionValues(CipherSpec.CIPHER_SPEC_OPTION);
			String[] finalValues = CommandUtil.toStringFromCsvs(cipherSpecParts);
			arguments.setCiphers(finalValues);
		}
		
	}
	
	private static void parseMacs(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(MacSpec.MAC_SPEC_OPTION)) {
			String[] macSpecParts = commandLine.getOptionValues(MacSpec.MAC_SPEC_OPTION);
			String[] finalValues = CommandUtil.toStringFromCsvs(macSpecParts);
			arguments.setHmacs(finalValues);
		}
		
	}
	
	private static void parseSecurityLevel(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(SecurityLevel.SECURITY_LEVEL_OPTION)) {
			String securityLevel = commandLine.getOptionValue(SecurityLevel.SECURITY_LEVEL_OPTION);
			arguments.setSecurityLevel(securityLevel);
		}
		
	}
	
	private static void parseCompression(CommandLine commandLine, SshClientArguments arguments) {
		
		if (commandLine.hasOption(Compression.COMPRESSION_OPTION)) {
			arguments.setCompression(true);
		}
	}
	
}
