package com.sshtools.common.sftp;

import java.io.IOException;

/**
 * Thrown when an operation that requires an ordinary file is presented with a directory.
 * 
 * @author Brett Smith
 */
public class FileIsDirectoryException extends IOException {

	private static final long serialVersionUID = 244584508925942734L;
	
	/**
     * Constructs the exception.
     */
    public FileIsDirectoryException() {
        super("File is a directory.");
    }
    
	/**
     * Constructs the exception.
     * 
     * @param msg String
     */
    public FileIsDirectoryException(String msg) {
        super(msg);
    }
}
