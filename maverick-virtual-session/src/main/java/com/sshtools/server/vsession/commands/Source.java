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

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.VirtualConsole;

public class Source extends Msh {

	public Source() {
		super("source", SUBSYSTEM_SHELL, "source <script>", null);
		setDescription("Run script in same process");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		this.commandFactory = console.getShell().getCommandFactory();
		String[] args = cli.getArgs();
		if (args.length != 2) {
			throw new IllegalArgumentException("Expects a single script as the argument.");
		} else {
			source(console, console.getCurrentDirectory().resolveFile(args[1]));
		}
	}
}