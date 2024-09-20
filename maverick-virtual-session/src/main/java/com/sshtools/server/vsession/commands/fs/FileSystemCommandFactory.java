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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.ShellCommand;

public class FileSystemCommandFactory extends CommandFactory<ShellCommand> {

	public FileSystemCommandFactory() {

		installShellCommands();
	}
	
	protected void installShellCommands() {
		
		commands.put("pwd", Pwd.class);
		commands.put("cd", Cd.class);
		commands.put("rm", Rm.class);
		commands.put("mv", Mv.class);
		commands.put("cp", Cp.class);
		commands.put("refresh", Refresh.class);
		commands.put("mkdir", Mkdir.class);
		commands.put("ls", Ls.class);
		commands.put("cat", Cat.class);
		commands.put("nano", Nano.class);
		commands.put("follow", Follow.class);
		
	}

	@Override
	protected void configureCommand(ShellCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		super.configureCommand(c, con);
	}

}
