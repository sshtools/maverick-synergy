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

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Mv extends ShellCommand {
	public Mv() {
		super("mv", ShellCommand.SUBSYSTEM_FILESYSTEM, UsageHelper.build("mv [options] <sourcePath> <targetPath>",
				"-v        Verbose. Display file names as they are moved"), "Move or rename a files or directories");
	}

	public void run(String[] args, final VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length < 3)
			throw new IOException("Not enough file names supplied.");
		AbstractFile target = process.getCurrentDirectory().resolveFile(args[args.length - 1]);

		if (args.length > 3 && (!target.exists() || !target.isDirectory())) {
			throw new IOException("Target must exist as a folder if multiple sources are specified.");
		}

		for (int i = 1; i < args.length - 1; i++) {
			AbstractFile src = process.getCurrentDirectory().resolveFile(args[i]);
			if (src.isDirectory() && target.isFile()) {
				throw new IOException("Cannot move folder " + src + " to file " + target);
			}

			if (!target.exists()) {
				src.moveTo(target);
			} else {
				src.moveTo(target.resolveFile(src.getName()));
			}
			if (CliHelper.hasShortOption(args, 'v')) {
				process.println(src.toString());
			}
		}
	}
}
