package com.sshtools.client.shell;

/*-
 * #%L
 * Client API
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

public interface ShellWriter {

	/**
	 * Interrupt the process by sending a Ctrl+C to the process.
	 *
	 * @throws IOException
	 */
	public abstract void interrupt() throws IOException;

	/**
	 * Send data to the remote command just like the user had typed it.
	 * @param string the typed key data
	 * @throws IOException
	 */
	public abstract void type(String string) throws IOException;

	/**
	 * Send a carriage return to the remote command.
	 * @throws IOException
	 */
	public abstract void carriageReturn() throws IOException;

	/**
	 * Send data to the remote command and finish with a carriage return.
	 * @param string String
	 * @throws IOException
	 */
	public abstract void typeAndReturn(String string) throws IOException;

}
