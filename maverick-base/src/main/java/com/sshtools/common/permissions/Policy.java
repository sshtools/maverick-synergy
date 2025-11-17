package com.sshtools.common.permissions;

public interface Policy {

	/**
	 * The type this permission set will be registered as.
	 * 
	 * @return type
	 */
	Class<? extends Policy> type();

}