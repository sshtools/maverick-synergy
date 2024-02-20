package com.sshtools.common.command;

/*-
 * #%L
 * Base API
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.components.Component;
import com.sshtools.common.ssh.components.ComponentInstanceFactory;

public interface ExecutableCommand extends Component {

	public interface ExecutableCommandFactory<T extends ExecutableCommand> extends ComponentInstanceFactory<T> {
	}

	/**
	 * Value returned from {@link #getExitCode()} to indicate that the process
	 * is still active.
	 */
	int STILL_ACTIVE = Integer.MIN_VALUE;

	/**
	 * Initialize the command. This can be overridden but always call this super
	 * method with the command's session.
	 * 
	 * @param session
	 */
	void init(SessionChannelServer session);

	SessionChannel getSession();

	/**
	 * Create the process but wait for the {@link #onStart()} method before
	 * performing any IO.
	 * 
	 * @param cmd
	 * @param environment
	 * @return boolean
	 */
	boolean createProcess(String[] args, Map<String, String> environment);

	/**
	 * Start the command.
	 *
	 */
	void start();

	/**
	 * Called once the command has been started. Operations within this method
	 * SHOULD NOT block as this will cause the connection to lockup.
	 */
	void onStart();

	/**
	 * Kill the command.
	 */
	void kill();

	/**
	 * Get the exit code for this process. If the process has not completed
	 * return {@link #STILL_ACTIVE}.
	 *
	 * @return int
	 */
	int getExitCode();

	/**
	 * Get the STDOUT OutputStream for this process.
	 * 
	 * @return OutputStream
	 */
	OutputStream getOutputStream();

	/**
	 * Get the STDERR OutputStream for this process.
	 * 
	 * @return OutputStream
	 */
	OutputStream getStderrOutputStream();

	/**
	 * Get the STDIN InputStream for this process.
	 * 
	 * @return InputStream
	 */
	InputStream getInputStream();

	boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height, byte[] modes);

}
