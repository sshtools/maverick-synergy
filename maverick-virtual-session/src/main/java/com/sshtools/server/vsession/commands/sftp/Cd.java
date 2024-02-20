package com.sshtools.server.vsession.commands.sftp;

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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Cd extends SftpCommand {

	public Cd() {
		super("cd", "SFTP", "cd", "Moves the working directory to a new directory");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		if (args.length > 2)
			throw new IllegalArgumentException("Too many arguments.");
		if (args.length > 1) {
			cdRemote(args[1]);
		} else {
			cdRemote(getRemoteHome());
		}
	}


	private void cdRemote(String directory) {
		try {
			this.sftp.cd(directory);
		} catch (SftpStatusException | SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	private String getRemoteHome() {
		try {
			return this.sftp.getHome();
		} catch (SftpStatusException | SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
