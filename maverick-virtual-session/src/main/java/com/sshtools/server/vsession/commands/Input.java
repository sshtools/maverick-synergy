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

import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Input extends ShellCommand {

	public Input() {
		super("input", ShellCommand.SUBSYSTEM_SHELL,
				UsageHelper.build("input <env> <prompt>"),
				 "Read a line of input from the user and place it into an environment variable");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException, UsageException {
		if(args.length < 2) {
			throw new UsageException("You must provde an environment variable name to place the user input into!");
		}
		String val = console.readLine(getPrompt(args));
		console.getEnvironment().put(args[1], val);
	}

	private String getPrompt(String[] args) {
		StringBuffer buf = new StringBuffer();
		for(int i=2;i<args.length;i++) {
			buf.append(args[i]);
			buf.append(" ");
		}
		return buf.toString();
	}	
}
