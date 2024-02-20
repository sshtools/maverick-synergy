package com.sshtools.common.events;

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

public class EventException extends RuntimeException {

	private static final long serialVersionUID = 8920551654296049197L;

	public EventException() {
		super();
	}

	public EventException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public EventException(String arg0) {
		super(arg0);
	}

	public EventException(Throwable arg0) {
		super(arg0);
	}

	
}
