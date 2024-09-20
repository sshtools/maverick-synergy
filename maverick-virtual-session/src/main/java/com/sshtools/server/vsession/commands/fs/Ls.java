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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Ls extends ShellCommand {
	
	
	public Ls() {
		super("ls", SUBSYSTEM_FILESYSTEM, UsageHelper.build("ls [options] path...",
				"-l, --long						        Show details for each individual file/folder",
				"-a, --all                              Show all files",
				"-d, --directory                        List directories themeselves, not their contents",
				"-x, --extended                         Show extended attributes"), 
				"List the contents of a directory.");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {
		
		AbstractFile dir = process.getCurrentDirectory();

		if (args.length == 1) {
			list(args, process, dir);
		} else {
			List<String> paths = new ArrayList<>();
			for(int i=1; i<args.length;i++) {
				if(!CliHelper.isOption(args[i], "ladx")) {
					paths.add(args[i]);
				}
			}
			if(paths.isEmpty()) {
				list(args, process, dir);
			} else {
				for (String path : paths) {
					list(args, process, dir.resolveFile(path));
				}
			}
		}
	}

	private void list(String[] args, VirtualConsole process, AbstractFile file) throws IOException, PermissionDeniedException {
		if (file.exists()) {
			if (file.isFile() || (file.isDirectory() && CliHelper.hasShortOption(args, 'd'))) {
				printFile(args, process, file);
			} else {
				List<AbstractFile> children = file.getChildren();
				Collections.sort(children,
						new Comparator<AbstractFile>() {

							public int compare(AbstractFile o1, AbstractFile o2) {
								return o1.getName().compareTo(o2.getName());
							}
						});
				for (AbstractFile child : children) {
					printFile(args, process, child);
				}
			}
		} else {
			process.println(getCommandName() + ": " + file.getName() + " does not exist.");
		}
	}

	protected void printFile(String[] args, VirtualConsole process, AbstractFile file) throws IOException, PermissionDeniedException {
		if (!file.isHidden() || CliHelper.hasOption(args,'a', "all")) {
			
			if (CliHelper.hasOption(args,'l', "all")) {
				SftpFileAttributes attrs = file.getAttributes();
				
				String lastModifiedTime = "";
				long size = 0;
				if (file.isFile()) {
					size = attrs.size().longValue();
				} else if (file.isDirectory()) {
					size = 0;
				}
				SimpleDateFormat df;
		        long mt = (attrs.lastModifiedTime().toMillis());
		        long now = System.currentTimeMillis();

		        if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
		            df = new SimpleDateFormat("MMM dd  yyyy", process.getConnection().getLocale());
		        } else {
		            df = new SimpleDateFormat("MMM dd HH:mm", process.getConnection().getLocale());
		        }

		        lastModifiedTime = df.format(new Date(mt));
				int linkCount = 0;
				process.println(String.format("%s %-3d %-8s %-8s %10d %-14s %-30s", 
						attrs.toPermissionsString(), 
						linkCount, 
						attrs.bestUsername(),
						attrs.bestGroup(),
						size, 
						lastModifiedTime, 
						file.getName()));
			} else {
				process.println(file.getName());
			}
			if(CliHelper.hasOption(args,'x', "extended")) {
				var attrs = file.getAttributes();
				for(var name : attrs.extendedAttributes().keySet()) {
					var val = attrs.extendedAttributes().get(name);
					process.println(String.format("%" + (CliHelper.hasShortOption(args,'l') ? 64 : 4)+ "s%s", "", name.toString() + "=" + (val == null ? "" : val.toString())));
				}
			}
		}
	}
}
