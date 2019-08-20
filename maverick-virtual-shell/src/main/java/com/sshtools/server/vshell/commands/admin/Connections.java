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
package com.sshtools.server.vshell.commands.admin;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Cell;
import com.sshtools.server.vshell.terminal.Row;
import com.sshtools.server.vshell.terminal.Table;

public class Connections<T extends AbstractFile> extends ShellCommand {

	public Connections() {
		super("con", ShellCommand.SUBSYSTEM_SSHD);
		setDescription("Show active connections");
	}

	public void run(CommandLine args, VirtualProcess process)
			throws IOException, PermissionDeniedException {

		Table table = new Table(process.getTerminal());
		Row header = new Row(new Cell<String>("UUID"));
		table.setHeader(header);
		for(SshConnection c : process.getConnection().getConnectionManager().getAllConnections()) {
			table.add(new Row(new Cell<String>(c.getUUID())));
		}
		
		table.render(process.getConsole());
	}

}
