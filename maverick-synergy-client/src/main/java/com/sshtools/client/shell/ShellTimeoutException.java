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

/**
 * <p>
 * Exception thrown when an operation times out.
 * </p>
 * 
 * @author Lee David Painter
 */
public class ShellTimeoutException extends Exception {

	private static final long serialVersionUID = -7736649465198590395L;

	ShellTimeoutException() {
		super("The shell operation timed out");
	}

	ShellTimeoutException(String str) {
		super(str);
	}
}
