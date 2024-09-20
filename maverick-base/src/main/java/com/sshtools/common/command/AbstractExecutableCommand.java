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

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SessionChannelServer;

/**
 * This class can be extended to provide a single executable command. Commands
 * are configured in the {@link com.maverick.sshd.ConfigurationContext} and they
 * make use of the input/output streams provided to communicate with the client.
 *
 * @author Lee David Painter
 */
public abstract class AbstractExecutableCommand implements ExecutableCommand {

	/** The session channel instance on which this command is being executed **/
	protected SessionChannelServer session;

	public AbstractExecutableCommand() {
	}

	/**
	 * Initialize the command. This can be overridden but always call this super
	 * method with the command's session.
	 * 
	 * @param session
	 */
	@Override
	public void init(SessionChannelServer session) {
		this.session = session;
		session.haltIncomingData();
	}

	@Override
	public SessionChannel getSession() {
		return session;
	}

	/**
	 * Create the process but wait for the {@link #onStart()} method before
	 * performing any IO.
	 * 
	 * @param cmd
	 * @param environment
	 * @return boolean
	 */
	@Override
	public abstract boolean createProcess(String[] args, Map<String, String> environment);

	/**
	 * Start the command.
	 *
	 */
	@Override
	public void start() {
		session.getConnection().executeTask(new Runnable() {
			public void run() {
				try {
					onStart();
				} catch (Throwable e) {
					Log.error("Consumed error from executable command", e);
				}
			}
		});
		
	}

	/**
	 * Called once the command has been started. Operations within this method
	 * SHOULD NOT block as this will cause the connection to lockup.
	 */
	@Override
	public abstract void onStart();

	/**
	 * Kill the command.
	 */
	@Override
	public abstract void kill();

	/**
	 * Get the exit code for this process. If the process has not completed
	 * return {@link #STILL_ACTIVE}.
	 *
	 * @return int
	 */
	@Override
	public abstract int getExitCode();

	/**
	 * Get the STDOUT OutputStream for this process.
	 * 
	 * @return OutputStream
	 */
	@Override
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}

	/**
	 * Get the STDERR OutputStream for this process.
	 * 
	 * @return OutputStream
	 */
	@Override
	public OutputStream getStderrOutputStream() {
		return session.getErrorStream();
	}

	/**
	 * Get the STDIN InputStream for this process.
	 * 
	 * @return InputStream
	 */
	@Override
	public InputStream getInputStream() {
		return session.getInputStream();
	}

	@Override
	public boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height, byte[] modes) {
		return false;
	}

}
