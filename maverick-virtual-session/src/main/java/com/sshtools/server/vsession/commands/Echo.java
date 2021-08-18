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

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: echo [-n] [string]
 * @author lee
 *
 */
public class Echo extends ShellCommand {

	public Echo() {
		super("echo", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("echo [options] <string>",
				"-n       Don't print newline"), "Echo a message to the screen");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		StringBuilder bui = new StringBuilder();

		for (int i = 1 ; i < args.length; i++) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			bui.append(args[i]);
		}
		if (CliHelper.hasShortOption(args, 'n')) {
			console.print(bui.toString());
		} else {
			console.println(bui.toString());
		}
	}
}
