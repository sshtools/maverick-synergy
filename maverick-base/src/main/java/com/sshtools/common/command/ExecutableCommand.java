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