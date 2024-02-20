package com.sshtools.synergy.jdk16;

/*-
 * #%L
 * Common code for Unix Domain Socket support
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

public class UnixDomainSockets {

	public static final String DIRECT_STREAM_LOCAL_CHANNEL = "direct-streamlocal@openssh.com";
	public static final String FORWARDED_STREAM_LOCAL_CHANNEL = "forwarded-streamlocal@openssh.com";
	
	public static final String CANCEL_STREAM_LOCAL_FORWARD_REQUEST = "cancel-streamlocal-forward@openssh.com";
	public static final String STREAM_LOCAL_FORWARD_REQUEST = "streamlocal-forward@openssh.com";
	
}
