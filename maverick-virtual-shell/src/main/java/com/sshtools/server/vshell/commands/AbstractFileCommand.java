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
package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;

public abstract class AbstractFileCommand extends ShellCommand {

	public AbstractFileCommand(String name, String subsystem) {
		super(name, subsystem);
	}

	public AbstractFileCommand(String name, String subsystem, String signature,
			Option... options) {
		super(name, subsystem, signature, options);
	}

	public AbstractFileCommand(String name, String subsystem, String signature) {
		super(name, subsystem, signature);
	}

	public int complete(String buffer, int cursor, List<String> candidates) {
		
		try {
			AbstractFile pwd = getProcess().getCurrentDirectory();
			for(AbstractFile f : pwd.getChildren()) {
				if(f.getName().startsWith(buffer)) {
					candidates.add(f.getName());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PermissionDeniedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

}
