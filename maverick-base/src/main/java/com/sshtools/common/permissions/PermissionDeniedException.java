package com.sshtools.common.permissions;

/**
 * Thrown when the file system denies access to a user whom does not have
 * permission to gain access to a file system object.
 * 
 * @author Lee David Painter
 */
public class PermissionDeniedException extends Exception {

	private static final long serialVersionUID = 7975609968862520326L;

	public PermissionDeniedException(String msg) {
		super(msg);
	}

	public PermissionDeniedException(String message, Throwable t) {
		super(message, t);
	}
}
