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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell.commands.fs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

public class Ls extends ShellCommand {
	
	
	public Ls() {
		super("ls", SUBSYSTEM_FILESYSTEM, "[<path1>,[,<path2>],..]", new Option("l", false, "Long format"), new Option("a", false,
			"All files including hidden ones"), new Option("x", false, "Show extended attributes"),
			new Option("d", false, "List directory entries instead of contents"));
		setDescription("Lists the contents of the current directory");
		setBuiltIn(false);
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException, PermissionDeniedException {
		Console term = process.getConsole();
		
		AbstractFile dir = process.getCurrentDirectory();
		String[] argArr = args.getArgs();
		if (argArr.length == 1) {
			list(args, term, process, dir);
		} else {
			for (int i = 1; i < argArr.length; i++) {
				list(args, term, process, dir.resolveFile(argArr[i]));
			}
		}
	}

	private void list(CommandLine cli, Console term, VirtualProcess process,AbstractFile file) throws IOException, PermissionDeniedException {
		if (file.exists()) {
			if (file.isFile() || (file.isDirectory() && cli.hasOption('d'))) {
				printFile(cli, term, process, file);
			} else {
				List<AbstractFile> children = file.getChildren();
				Collections.sort(children,
						new Comparator<AbstractFile>() {

							public int compare(AbstractFile o1, AbstractFile o2) {
								return o1.getName().compareTo(o2.getName());
							}
						});
				for (AbstractFile child : children) {
					printFile(cli, term, process, child);
				}
			}
		} else {
			term.printStringNewline(getCommandName() + ": " + file.getName() + " does not exist.");
		}
	}

	protected void printFile(CommandLine cli, Console term, VirtualProcess process, AbstractFile file) throws IOException, PermissionDeniedException {
		if (!file.isHidden() || cli.hasOption('a')) {
			StringBuffer attr = new StringBuffer();
			
			if (cli.hasOption('l')) {
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
				term.printStringNewline(String.format("%s %-3d %-8s %-8s %10d %-14s %-30s", attrs.getPermissionsString(), linkCount, attrs.getUID(), attrs.getGID(), size, lastModifiedTime, file.getName()) + attr.toString());
			} else {
				term.printStringNewline(file.getName() + attr.toString());
			}
			if(cli.hasOption('x')) {
				SftpFileAttributes attrs = file.getAttributes();
				for(Object name : attrs.getExtendedAttributes().keySet()) {
					Object val = attrs.getExtendedAttributes().get(name);
					term.printStringNewline(String.format("%" + (cli.hasOption('l') ? 64 : 4)+ "s%s", "", name.toString() + "=" + (val == null ? "" : val.toString())));
				}
			}
		}
	}
}
