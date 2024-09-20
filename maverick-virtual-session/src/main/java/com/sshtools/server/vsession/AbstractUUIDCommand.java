package com.sshtools.server.vsession;

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

import java.util.List;

import com.sshtools.common.ssh.SshConnection;

public abstract class AbstractUUIDCommand extends ShellCommand {

	public AbstractUUIDCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}

	public int complete(String buffer, int cursor, List<String> candidates, VirtualConsole console) {
		
		for(SshConnection c : console.getConnection().getConnectionManager().getAllConnections()) {
			if(buffer==null || c.getUUID().startsWith(buffer)) {
				candidates.add(c.getUUID());
			}
		}
		return 0;
	}

}
