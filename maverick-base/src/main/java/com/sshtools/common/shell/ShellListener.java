/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
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
