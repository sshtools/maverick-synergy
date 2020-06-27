package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.SshContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.vsession.commands.ssh.SshClientOptions.CipherSpec;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Compression;
import com.sshtools.vsession.commands.ssh.SshClientOptions.IdentityFile;
import com.sshtools.vsession.commands.ssh.SshClientOptions.LoginName;
import com.sshtools.vsession.commands.ssh.SshClientOptions.MacSpec;
import com.sshtools.vsession.commands.ssh.SshClientOptions.Port;
import com.sshtools.vsession.commands.ssh.SshClientOptions.SecurityLevel;

public class AbstractSshOptionsEvaluator {

	static List<SshOptionsResolver> resolvers = new ArrayList<>();
	
	public static void addResolver(SshOptionsResolver resolver) {
		resolvers.add(resolver);
	}
	
	protected static void parseDestination(CommandLine commandLine, SshClientArguments arguments) {

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
			if(resolver.resolveDestination(destination, arguments)) {
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
	
	protected static void parseIdentityFilename(CommandLine commandLine, SshClientArguments arguments) {

		if (commandLine.hasOption(IdentityFile.IDENTITY_FILE_OPTION)) {
			arguments.setIdentityFile(commandLine.getOptionValue(IdentityFile.IDENTITY_FILE_OPTION));
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
