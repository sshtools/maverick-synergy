package com.sshtools.server.vsession;

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
