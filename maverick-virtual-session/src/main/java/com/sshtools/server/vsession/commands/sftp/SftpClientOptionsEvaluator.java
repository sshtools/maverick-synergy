package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.vsession.commands.ssh.AbstractSshOptionsEvaluator;
import com.sshtools.vsession.commands.ssh.SshClientArguments;

public class SftpClientOptionsEvaluator extends AbstractSshOptionsEvaluator{
	
	public static SshClientArguments evaluate(CommandLine commandLine, VirtualConsole console) throws IOException, PermissionDeniedException {

		if (Log.isDebugEnabled()) {
			Log.debug("The argument list passed as {}", commandLine.getArgList());

			List<String> optionList = Arrays.asList(commandLine.getOptions()).stream()
					.map(option -> String.format("{} -> {}", option.getArgName(), option.getValue()))
					.collect(Collectors.toList());

			Log.debug("The option list passed as {}", optionList);
		}

		SshClientArguments arguments = new SshClientArguments();

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

}
