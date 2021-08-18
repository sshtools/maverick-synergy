/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.command;

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
public abstract class ExecutableCommand {

	/**
	 * Value returned from {@link #getExitCode()} to indicate that the process
	 * is still active.
	 */
	public static final int STILL_ACTIVE = Integer.MIN_VALUE;

	/** The session channel instance on which this command is being executed **/
	protected SessionChannelServer session;

	public ExecutableCommand() {
	}

	/**
	 * Initialize the command. This can be overridden but always call this super
	 * method with the command's session.
	 * 
	 * @param session
	 */
	public void init(SessionChannelServer session) {
		this.session = session;
		session.haltIncomingData();
	}

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
	public abstract boolean createProcess(String[] args, Map<String, String> environment);

	/**
	 * Start the command.
	 *
	 */
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
	public abstract void onStart();

	/**
	 * Kill the command.
	 */
	public abstract void kill();

	/**
	 * Get the exit code for this process. If the process has not completed
	 * return {@link #STILL_ACTIVE}.
	 *
	 * @return int
	 */
	public abstract int getExitCode();

	/**
	 * Get the STDOUT OutputStream for this process.
	 * 
	 * @return OutputStream
	 */
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}

	/**
	 * Get the STDERR OutputStream for this process.
	 * 
	 * @return OutputStream
	 */
	public OutputStream getStderrOutputStream() {
		return session.getErrorStream();
	}

	/**
	 * Get the STDIN InputStream for this process.
	 * 
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		return session.getInputStream();
	}

	public boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height, byte[] modes) {
		return false;
	}

}
