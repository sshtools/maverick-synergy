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

package com.sshtools.server.vsession.commands.fs;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: cd [directory]
 * @author lee
 */
public class Cd extends ShellCommand {
	public Cd() {
		super("cd", ShellCommand.SUBSYSTEM_FILESYSTEM, "cd <directory>", "Moves the working directory to a new directory");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length > 2)
			throw new IllegalArgumentException("Too many arguments.");
		if (args.length > 1) {
			process.setCurrentDirectory(args[1]);
		} else {
			try {
				process.setCurrentDirectory(process.getEnvironment().getOrDefault("HOME", "").toString());
			} catch (PermissionDeniedException e) {
				throw new IllegalAccessError();
			}
		}
	}
}
