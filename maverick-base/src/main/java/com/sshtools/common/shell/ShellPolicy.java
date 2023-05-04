package com.sshtools.common.shell;

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
