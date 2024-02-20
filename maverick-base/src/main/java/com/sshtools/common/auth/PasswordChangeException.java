package com.sshtools.common.auth;

/**
 * Thrown when the login process requires that the user change their password.
 * 
 * 
 */
public class PasswordChangeException extends Exception {

	private static final long serialVersionUID = 2434493043273251078L;

	public PasswordChangeException() {
	}

	public PasswordChangeException(String msg) {
		super(msg);
	}
}
