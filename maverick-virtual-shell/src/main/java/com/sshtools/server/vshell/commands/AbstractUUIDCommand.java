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

import java.util.List;

import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public abstract class AbstractUUIDCommand<T extends AbstractFile> extends ShellCommand {

	public AbstractUUIDCommand(String name, String subsystem) {
		super(name, subsystem);
	}

	public AbstractUUIDCommand(String name, String subsystem, String signature) {
		super(name, subsystem, signature);
	}

	public AbstractUUIDCommand(String name, String subsystem, String signature,
			Option... options) {
		super(name, subsystem, signature, options);
	}

	public int complete(String buffer, int cursor, List<String> candidates, VirtualProcess process) {
		
		for(SshConnection c : process.getConnection().getConnectionManager().getAllConnections()) {
			if(buffer==null || c.getUUID().startsWith(buffer)) {
				candidates.add(c.getUUID());
			}
		}
		return 0;
	}

}