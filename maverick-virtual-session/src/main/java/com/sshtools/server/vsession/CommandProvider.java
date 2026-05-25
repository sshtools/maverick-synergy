package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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
import java.lang.reflect.InvocationTargetException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public interface CommandProvider<T extends Command> {

	/**
	 * Get the names of the supported commands.
	 * 
	 * @return java.util.Set<String>
	 */
	public java.util.Set<String> getSupportedCommands();

	/**
	 * Create a new instance of the command with the given name.
	 * 
	 * @param command
	 *            String
	 * @return T
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 * @throws UnsupportedCommandException 
	 */
	public T createCommand(String command, SshConnection con) throws UnsupportedCommandException;

	
	public boolean supportsCommand(String command);


}
