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

package com.sshtools.server.vsession.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Catch extends Msh {

	public Catch() {
		super("catch", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("catch <command> <arg1> <arg2>.."),
				null);
		setDescription("Run a command, catching exceptions it might throw");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException {
		
		setCommandFactory(console.getShell().getCommandFactory());

		if (args.length < 2) {
			throw new IllegalArgumentException(
					"Expects at least a command name as an argument.");
		} else {
			try {
				String[] pArgs = new String[args.length - 1];
				System.arraycopy(args, 1, pArgs, 0, pArgs.length);
				doSpawn(console, pArgs, false);
			} catch (Exception e) {
				if (CliHelper.hasShortOption(args, 'x')) {
					console.print(e);
				} else {
					console.println(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
				}
				if (CliHelper.hasShortOption(args, 't')) {
					throw new IOException("An error occured. " + e.getMessage());
				}
			}
		}
	}

}
