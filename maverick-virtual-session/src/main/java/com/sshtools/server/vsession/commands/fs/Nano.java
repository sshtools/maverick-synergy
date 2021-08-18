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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.common.files.nio.AbstractFileURI;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Nano extends ShellCommand {

	public Nano() {
		super("nano", SUBSYSTEM_FILESYSTEM, "nano <file>", "File editor");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		Map<String,Object> env = new HashMap<>();
		env.put("connection", console.getConnection());
		FileSystem fs = FileSystems.newFileSystem(
				AbstractFileURI.create(console.getConnection(), ""), 
					env,
					getClass().getClassLoader());

		org.jline.builtins.Nano n = new org.jline.builtins.Nano(console.getTerminal(), 
				fs.getPath(console.getCurrentDirectory().getAbsolutePath()));
		
		List<String> arglist = new ArrayList<>();
		if(args.length > 1) {
			for(int i=1;i<args.length;i++) {
				arglist.add(args[i]);
			}
		}

		n.open(arglist);
		n.run();
		
	}

}
