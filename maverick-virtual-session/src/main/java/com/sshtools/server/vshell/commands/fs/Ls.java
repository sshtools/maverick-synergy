/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Ls extends ShellCommand {
	
	
	public Ls() {
		super("ls", SUBSYSTEM_FILESYSTEM, "[<path1>,[,<path2>],..]", "Lists the contents of the current directory");
//		new Option("l", false, "Long format"), new Option("a", false,
//			"All files including hidden ones"), new Option("x", false, "Show extended attributes"),
//			new Option("d", false, "List directory entries instead of contents"));
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {
		
		AbstractFile dir = process.getCurrentDirectory();

		if (args.length == 1) {
			list(args, process, dir);
		} else {
			for (int i = 1; i < args.length; i++) {
				list(args, process, dir.resolveFile(args[i]));
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
		if (!file.isHidden() || CliHelper.hasShortOption(args,'a')) {
			StringBuffer attr = new StringBuffer();
			
			if (CliHelper.hasShortOption(args,'l')) {
				SftpFileAttributes attrs = file.getAttributes();
				
				String lastModifiedTime = "";
				long size = 0;
				if (file.isFile()) {
					size = attrs.getSize().longValue();
				} else if (file.isDirectory()) {
					if (file.isReadable()) {
						try {
							size = file.getChildren().size();
						}
						catch(IOException e) {
							// Unreadable
							size = 0;
						}
					}
				}
				SimpleDateFormat df;
		        long mt = (attrs.getModifiedTime().longValue() * 1000L);
		        long now = System.currentTimeMillis();

		        if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
		            df = new SimpleDateFormat("MMM dd  yyyy", process.getConnection().getLocale());
		        } else {
		            df = new SimpleDateFormat("MMM dd HH:mm", process.getConnection().getLocale());
		        }

		        lastModifiedTime = df.format(new Date(mt));
				int linkCount = 0;
				process.println(String.format("%s %-3d %-8s %-8s %10d %-14s %-30s", attrs.getPermissionsString(), linkCount, attrs.getUID(), attrs.getGID(), size, lastModifiedTime, file.getName()) + attr.toString());
			} else {
				process.println(file.getName() + attr.toString());
			}
			if(CliHelper.hasShortOption(args,'x')) {
				SftpFileAttributes attrs = file.getAttributes();
				for(Object name : attrs.getExtendedAttributes().keySet()) {
					Object val = attrs.getExtendedAttributes().get(name);
					process.println(String.format("%" + (CliHelper.hasShortOption(args,'l') ? 64 : 4)+ "s%s", "", name.toString() + "=" + (val == null ? "" : val.toString())));
				}
			}
		}
	}
}
