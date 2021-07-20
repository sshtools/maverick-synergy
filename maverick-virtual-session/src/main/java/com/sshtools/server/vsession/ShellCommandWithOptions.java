
package com.sshtools.server.vsession;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.sshtools.common.permissions.PermissionDeniedException;

public abstract class ShellCommandWithOptions extends ShellCommand {

	private Options options = new Options();
	
	public ShellCommandWithOptions(String name, String subsystem, String signature, String description,
			Option... options) {
		super(name, subsystem, signature, description);
		if (options != null) {
			for (Option option : options) {
				this.options.addOption(option);
			}
		}
	}

	@Override
	public final void run(String[] args, VirtualConsole console) throws IOException, PermissionDeniedException, UsageException {
		
		DefaultParser parser = new DefaultParser();
		Options options = getOptions();
		if (options == null) {
			options = new Options();
		}
		
		CommandLine cli;
		try {
			cli = parser.parse(options, filterArgs(args), !hasFixedOptions());
		} catch (ParseException e) {
			throw new UsageException(getUsage());
		}
		
		run (cli, console);
	}

	protected String[] filterArgs(String[] args) {
		return args;
	}

	public abstract void run(CommandLine cli, VirtualConsole console) throws IOException, PermissionDeniedException, UsageException;
	
	public Options getOptions() {
		return options;
	}
	
	public boolean hasFixedOptions() {
		return true;
	}
}
