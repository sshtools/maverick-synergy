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
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Date extends ShellCommand {

	public Date() {
		super("date", ShellCommand.SUBSYSTEM_SHELL, 
				UsageHelper.build("date [options]",
						"-t   Just output time",
						"-d   Just output date",
						"-l   Output date and time in long format",
						"-f   Use a custom format"), 
				"Display the current date");
		
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException, UsageException {
		DateFormat fmt = CliHelper.hasShortOption(args, 'l') ? DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG) : DateFormat
			.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		if (CliHelper.hasShortOption(args, 't')) {
			if (CliHelper.hasShortOption(args, 'l')) {
				fmt = DateFormat.getTimeInstance(DateFormat.LONG);
			} else {
				fmt = DateFormat.getTimeInstance(DateFormat.SHORT);
			}
		} else if (CliHelper.hasShortOption(args, 'd')) {
			if (CliHelper.hasShortOption(args, 'l')) {
				fmt = DateFormat.getDateInstance(DateFormat.LONG);
			} else {
				fmt = DateFormat.getDateInstance(DateFormat.SHORT);
			}
		} else if (CliHelper.hasShortOption(args, 'f')) {
			fmt = new SimpleDateFormat(CliHelper.getShortValue(args, 'f'));
		}
		console.println(fmt.format(new java.util.Date()));
	}
}
