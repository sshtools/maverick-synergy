package com.sshtools.server.vsession;

import com.sshtools.common.permissions.PermissionType;

public enum ShellPermission implements PermissionType {

	EXECUTE("exec"),
	CHANGE_PASSWORD("changePassword"),
	SET_PASSWORD("setPassword");
	
	private final String val;
	
	private ShellPermission(final String val) {
		this.val = val;
	}
	
	public String toString() {
		return val;
	}

	public String getName() {
		return val;
	}
}
