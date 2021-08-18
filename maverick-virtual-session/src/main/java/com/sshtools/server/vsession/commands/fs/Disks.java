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

import java.io.File;
import java.io.IOException;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Disks extends ShellCommand {
	public Disks() {
		super("disks", SUBSYSTEM_FILESYSTEM, UsageHelper.build("disks [options] <path>", 
				"-m               Display size in megabytes",
				"-k               Display size in kilobytes",
				"-g               Display size in gigabytes",
				"-b               Display size in bytes"),
				"List local root file systems or displays information about local disk drives");

	}

	public void run(String[] args, VirtualConsole process) throws IOException {

		if (args.length == 1) {
			for (File file : File.listRoots()) {
				process.println(file.getAbsolutePath());
			}
		} else {
			for (int i = 1; i < args.length; i++) {
				File f = new File(args[i]);
				process.println(f.getAbsolutePath());
				process.println("    Total: " + format(args, f.getTotalSpace()));
				process.println("    Usable: " + format(args, f.getUsableSpace()));
				process.println("    Free: " + format(args, f.getFreeSpace()));
			}
		}

	}

	static String format(String[] args, long value) {
		if (CliHelper.hasShortOption(args, 'm')) {
			return (value / 1024 / 1024) + " MB";
		} else if (CliHelper.hasShortOption(args, 'k')) {
			return (value / 1024) + " KB";
		} else if (CliHelper.hasShortOption(args, 'b')) {
			return value + " Bytes";
		} else {
			return (value / 1024 / 1024 / 1024) + " GB";
		}
	}
}
