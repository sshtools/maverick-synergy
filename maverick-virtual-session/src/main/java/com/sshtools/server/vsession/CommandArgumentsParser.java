package com.sshtools.server.vsession;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandArgumentsParser {

	public static CommandLine parse(Options options, String[] args, String usage) {
		DefaultParser parser = new DefaultParser();

		if (options == null) {
			options = new Options();
		}
		
		CommandLine commandLine;
		try {
			commandLine = parser.parse(options, args, false);
		} catch (ParseException e) {
			throw new IllegalArgumentException(usage);
		}
		
		return commandLine;
	}
}
