
package com.sshtools.agent.exceptions;

public class KeyTimeoutException extends Exception {

	private static final long serialVersionUID = -5137438842017907906L;

	/**
     * Creates a new KeyTimeoutException object.
     */
    public KeyTimeoutException() {
        super("The key has timed out");
    }
}
