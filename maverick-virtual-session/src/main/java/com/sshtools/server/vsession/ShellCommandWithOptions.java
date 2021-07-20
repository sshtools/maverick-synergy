/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
