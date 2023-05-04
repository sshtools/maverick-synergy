package com.sshtools.common.permissions;

public class Permissions {

	protected long permissions;

	public Permissions() {
		super();
	}

	public Permissions(long permissions) {
		this.permissions = permissions;
	}
	
	public void add(int permission) {
		permissions |= permission;
	}

	public void remove(int permission) {
		permissions &= ~permission;
	}

	public boolean check(int permission) {
		return (permissions & permission) == permission;
	}

}