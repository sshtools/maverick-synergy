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
package com.sshtools.server.vsession.commands.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Cat extends ShellCommand {
	public Cat() {
		super("cat", ShellCommand.SUBSYSTEM_SHELL, "", "");
//		UsageHelper.build("cat <filename>...",
//				new Option("E", false, "Display $ at end of each line"),
//				new Option("n", "number", false, "number all output lines"),
//				new Option("s", "squeeze-blank", false, "suppress repeated empty output lines"),
//				new Option("T", "show-tabs", false, "displays TAB characters as ^I"),
//				new Option("v", "show-nonprinting", false, "use ^ and M- notation, except for LFD and TAB"));
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length < 2)
			throw new IOException("At least one argument required");
		for (int i = 1; i < args.length; i++) {
			AbstractFile obj = process.getCurrentDirectory().resolveFile(args[i]);
			BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getInputStream()));
			
			try {
				String line = null;
				int n = 1;
				boolean lastLineBlank = false;
				while ((line = reader.readLine()) != null) {
					if(lastLineBlank && line.equals("") && CliHelper.hasShortOption(args, 's')) {
						continue;
					}
					lastLineBlank = line.equals("");
					if(CliHelper.hasShortOption(args, 'n')) {
						process.println("  " + n++ + " ");
					}
					if(CliHelper.hasShortOption(args, 'v')) {
						for(int x=0;x<line.length();x++) {
							char c = line.charAt(x);
							if((c & 0x80) == 0x80) {
								c &= ~0x80;
							}
							if(c == 9 || c == 10) {
								process.print(c);
							} else if(c < 32) {
								process.print('^');
								process.print((char) (c+64));
							} else if(c < 127) {
								process.print(c);
							} else {
								process.print("^?");
							}
						}
					} else {
						process.print(line);
					}
					if(CliHelper.hasShortOption(args, 'E')) {
						process.println("$");
					} else {
						process.println();
					}

				}
			} finally {
				reader.close();
			}
		}
	}
	

}
