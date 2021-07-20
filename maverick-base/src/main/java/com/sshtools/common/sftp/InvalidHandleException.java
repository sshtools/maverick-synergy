

package com.sshtools.common.sftp;

/**
 * Thrown when an invalid file handle is received.
 * 
 * @author Lee David Painter
 */
public class InvalidHandleException extends Exception {

	private static final long serialVersionUID = 695631098889371849L;

	/**
     * Constructs the exception.
     * 
     * @param msg String
     */
    public InvalidHandleException(String msg) {
        super(msg);
    }
}
