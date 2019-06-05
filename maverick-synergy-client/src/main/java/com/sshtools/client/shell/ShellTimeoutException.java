/* HEADER */
package com.sshtools.client.shell;

/**
 * <p>
 * Exception thrown when an operation times out.
 * </p>
 * 
 * @author Lee David Painter
 */
public class ShellTimeoutException extends Exception {

	private static final long serialVersionUID = -7736649465198590395L;

	ShellTimeoutException() {
		super("The shell operation timed out");
	}

	ShellTimeoutException(String str) {
		super(str);
	}
}
