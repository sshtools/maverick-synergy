package com.sshtools.common.shell;

import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.ssh.SshConnection;

public class ShellPolicy extends Permissions {

	public static final int SHELL							  = 0x00001000;
	public static final int EXEC							  = 0x00002000;
	public static final int SUBSYSTEM						  = 0x00004000;
	
	int sessionTimeoutSeconds = 0;
	
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
}
