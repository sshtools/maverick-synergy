package com.sshtools.server.vsession.commands.fs;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
