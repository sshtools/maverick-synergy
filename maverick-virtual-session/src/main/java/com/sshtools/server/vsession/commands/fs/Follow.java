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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Follow extends ShellCommand {
	public Follow() {
		super("follow", ShellCommand.SUBSYSTEM_FILESYSTEM, "follow <filename>", "Monitor a file");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process)
			throws IOException, PermissionDeniedException {

		if (args.length != 2)
			throw new IOException("A single argument is required");
		try {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(
					args[1]);
			long _filePointer = 0;
			while (true) {
				Thread.sleep(2000);
				long len = obj.getAttributes().getSize().longValue();
				if (len < _filePointer) {
					process.println(
									"--- TRUNCATED ---   File was reset. Restarting following from start of file.");
					_filePointer = len;
				} else if (len > _filePointer) {
					InputStream in = obj.getInputStream();
					in.skip(_filePointer);
					try(BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
						String line = null;
						while ((line = r.readLine()) != null) {
							process.println(line);
						}
						_filePointer = len;
					} finally {
						in.close();
					}
				}
			}
		} catch (Exception e) {
			process.println("--- ERROR ---   Fatal error following file, following stopped.");
		}
	}
}