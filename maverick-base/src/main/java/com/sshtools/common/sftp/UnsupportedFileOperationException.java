

package com.sshtools.common.sftp;

/**
 * Thrown when the file system does not support a requested operation.
 * 
 * @author Lee David Painter
 */
public class UnsupportedFileOperationException extends Exception {

	private static final long serialVersionUID = -3108058839489617756L;

	public UnsupportedFileOperationException(String msg) {
		super(msg);
	}
}
