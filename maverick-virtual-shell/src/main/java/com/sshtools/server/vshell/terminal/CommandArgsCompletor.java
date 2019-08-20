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
package com.sshtools.server.vshell.terminal;

import java.io.IOException;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.Command;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

import jline.Completor;

public class CommandArgsCompletor implements Completor {
	final 

	VirtualProcess process;
	
	public CommandArgsCompletor(VirtualProcess process) {
		this.process = process;
	}
	
	public int complete(String buffer, int cursor, List<String> candidates) {
		String search = buffer;
		Command command = CommandCompletor.command.get();
		if (command == null) {
			return -1;
		}

		// Do file completion for all file system commands
		if (command.getSubsystem().equals(ShellCommand.SUBSYSTEM_FILESYSTEM)) {
			AbstractFile obj = process.getCurrentDirectory();
			String path = null;
			if (search != null) {
				int lastSlash = search.lastIndexOf('/');
				if (lastSlash != -1) {
					path = search.substring(0, lastSlash + 1);
					try {
						obj = obj.resolveFile(path);
						search = search.substring(lastSlash + 1);
					} catch (Exception e) {
						Log.error("Failed to list directory.");
						obj = null;
					}
				}
			}
			if (obj != null) {
				try {
					
					try {
						for (AbstractFile child : obj.getChildren()) {
							if (search == null || child.getName().startsWith(search)) {
								candidates.add((path == null ? "" : path) + child.getName());
							}
						}
					} catch (PermissionDeniedException e) {
					}

				} catch (IOException fse) {
					Log.error("Failed to complete file path.", fse);
				}
			}
		}

		return command.complete(buffer, cursor, candidates);
	}

}
