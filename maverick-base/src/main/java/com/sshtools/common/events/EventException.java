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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.events;

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
