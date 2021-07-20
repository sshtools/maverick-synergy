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

package com.sshtools.common.shell;

public interface ShellListener {

	/**
	 * If the client requests a pseudo terminal for the session this method will
	 * be invoked before the shell, exec or subsystem is started.
	 * 
	 * @param term
	 * @param cols
	 * @param rows
	 * @param width
	 * @param height
	 * @param modes
	 * @return boolean
	 */
	boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height, byte[] modes);

	/**
	 * When the window (terminal) size changes on the client side, it MAY send
	 * notification in which case this method will be invoked to notify the
	 * session that a change has occurred.
	 * 
	 * @param cols
	 * @param rows
	 * @param width
	 * @param height
	 */
	void changeWindowDimensions(int cols, int rows, int width, int height);

	/**
	 * A signal can be delivered to the process by the client. If a signal is
	 * received this method will be invoked so that the session may evaluate and
	 * take the required action.
	 * 
	 * @param signal
	 */
	void processSignal(String signal);

	/**
	 * If the client requests that an environment variable be set this method
	 * will be invoked.
	 * 
	 * @param name
	 * @param value
	 * @return <tt>true</tt> if the variable has been set, otherwise
	 *         <tt>false</tt>
	 */
	boolean setEnvironmentVariable(String name, String value);
}
