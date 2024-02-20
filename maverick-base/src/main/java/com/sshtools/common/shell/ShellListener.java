package com.sshtools.common.shell;

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
