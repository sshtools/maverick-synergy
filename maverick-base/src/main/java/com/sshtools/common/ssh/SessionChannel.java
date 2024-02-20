package com.sshtools.common.ssh;

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

import com.sshtools.common.util.UnsignedInteger32;

public interface SessionChannel extends Channel {

	SshConnection getConnection();

	void haltIncomingData();

	void resumeIncomingData();
	
	UnsignedInteger32 getMaximumWindowSpace();

	UnsignedInteger32 getMinimumWindowSpace();

	/**
	 * Called once the session is open and data can be sent/received. This event
	 * happens once the user has either started the shell or executed a command.
	 */
	void onSessionOpen();
	
	InputStream getInputStream();
	
	OutputStream getOutputStream();
	
}
