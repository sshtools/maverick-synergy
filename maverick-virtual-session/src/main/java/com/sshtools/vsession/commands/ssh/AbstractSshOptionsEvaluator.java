/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.jline.reader.Candidate;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.vsession.commands.ssh.SshClientOptions.CipherSpec;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Compression;
import com.sshtools.vsession.commands.ssh.SshClientOptions.IdentityFile;
import com.sshtools.vsession.commands.ssh.SshClientOptions.LoginName;
import com.sshtools.vsession.commands.ssh.SshClientOptions.MacSpec;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Port;
import com.sshtools.vsession.commands.ssh.SshClientOptions.SecurityLevel;

public class AbstractSshOptionsEvaluator {

	static Set<SshOptionsResolver> resolvers = new HashSet<>();
	
	public static void addResolver(SshOptionsResolver resolver) {
		resolvers.add(resolver);
	}
	
	protected static void complete(String destination, List<Candidate> candidates) {
		for(SshOptionsResolver resolver : resolvers) {
			for(String candidate : resolver.matchDestinations(destination)) {
				candidates.add(new Candidate(candidate));
			}
		}
	}
	
	protected static void parseDestination(CommandLine commandLine, SshClientArguments arguments) throws IOException {

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
		
		
		for(SshOptionsResolver resolver : resolvers) {
			if(resolver.resolveOptions(destination, arguments)) {
				break;
			}
		}
	}
	
	protected static void parsePort(CommandLine commandLine, SshClientArguments arguments) {
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
	
	protected static void parseLoginName(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(LoginName.LOGIN_NAME_OPTION)) {
			arguments.setLoginName(commandLine.getOptionValue(LoginName.LOGIN_NAME_OPTION));
		}
		
	}
	
	protected static void parseIdentityFilename(CommandLine commandLine, SshClientArguments arguments, VirtualConsole console) throws IOException, PermissionDeniedException {

		if (commandLine.hasOption(IdentityFile.IDENTITY_FILE_OPTION)) {
			String filename = commandLine.getOptionValue(IdentityFile.IDENTITY_FILE_OPTION);
			AbstractFile file = console.getCurrentDirectory().resolveFile(filename);
			if(!file.exists()) {
				throw new IllegalArgumentException(filename + " does not exist");
			}
			arguments.setIdentityFile(file);
		}
		
	}
	
	protected static void parseCiphers(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(CipherSpec.CIPHER_SPEC_OPTION)) {
			String[] cipherSpecParts = commandLine.getOptionValues(CipherSpec.CIPHER_SPEC_OPTION);
			String[] finalValues = CommandUtil.toStringFromCsvs(cipherSpecParts);
			arguments.setCiphers(finalValues);
		}
		
	}
	
	protected static void parseMacs(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(MacSpec.MAC_SPEC_OPTION)) {
			String[] macSpecParts = commandLine.getOptionValues(MacSpec.MAC_SPEC_OPTION);
			String[] finalValues = CommandUtil.toStringFromCsvs(macSpecParts);
			arguments.setHmacs(finalValues);
		}
		
	}
	
	protected static void parseSecurityLevel(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(SecurityLevel.SECURITY_LEVEL_OPTION)) {
			String securityLevel = commandLine.getOptionValue(SecurityLevel.SECURITY_LEVEL_OPTION);
			arguments.setSecurityLevel(securityLevel);
		}
		
	}
	
	protected static void parseCompression(CommandLine commandLine, SshClientArguments arguments) {
		
		if (commandLine.hasOption(Compression.COMPRESSION_OPTION)) {
			arguments.setCompression(true);
		}
	}

	protected static Collection<SshOptionsResolver> getResolvers() {
		return Collections.unmodifiableCollection(resolvers);
	}
	
	
}
