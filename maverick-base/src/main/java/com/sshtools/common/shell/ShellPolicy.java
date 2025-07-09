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

import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger32;

public class ShellPolicy extends Permissions {

	public static final int SHELL							  = 0x00001000;
	public static final int EXEC							  = 0x00002000;
	public static final int SUBSYSTEM						  = 0x00004000;
	
	int sessionTimeoutSeconds = 0;
	protected int sessionMaxPacketSize = 65536;
	protected UnsignedInteger32 sessionMaxWindowSize = new UnsignedInteger32(1024000);
	protected UnsignedInteger32 sessionMinWindowSize = new UnsignedInteger32(131072);
	
	public ShellPolicy() {
		permissions = SHELL
		| EXEC
		| SUBSYSTEM;	
	}

	protected boolean assertPermission(SshConnection con, int perm, String... args) {
		return check(perm);
	}

	public final boolean checkPermission(SshConnection con, int perm, String... args) {
		return assertPermission(con, perm, args);
	}
	
	/**
	 * Returns the session timeout in seconds
	 * 
	 * @return int
	 */
	public int getSessionTimeout() {
		return sessionTimeoutSeconds;
	}

	/**
	 * Sets the session timeout in seconds
	 * 
	 * @param sessionTimeoutSeconds
	 *            int
	 */
	public void setSessionTimeout(int sessionTimeoutSeconds) {
		this.sessionTimeoutSeconds = sessionTimeoutSeconds;
	}

	public int getSessionTimeoutSeconds() {
		return sessionTimeoutSeconds;
	}

	public void setSessionTimeoutSeconds(int sessionTimeoutSeconds) {
		this.sessionTimeoutSeconds = sessionTimeoutSeconds;
	}

	public int getSessionMaxPacketSize() {
		return sessionMaxPacketSize;
	}

	public void setSessionMaxPacketSize(int sessionMaxPacketSize) {
		this.sessionMaxPacketSize = sessionMaxPacketSize;
	}

	public UnsignedInteger32 getSessionMaxWindowSize() {
		return sessionMaxWindowSize;
	}

	public void setSessionMaxWindowSize(UnsignedInteger32 sessionMaxWindowSize) {
		this.sessionMaxWindowSize = sessionMaxWindowSize;
	}

	public UnsignedInteger32 getSessionMinWindowSize() {
		return sessionMinWindowSize;
	}

	public void setSessionMinWindowSize(UnsignedInteger32 sessionMinWindowSize) {
		this.sessionMinWindowSize = sessionMinWindowSize;
	}
	
}
