package com.sshtools.client.sftp;

/**
 * Exception thrown when a file transfer is cancelled.
 */
public class TransferCancelledException extends Exception {

	private static final long serialVersionUID = -2731472071199529280L;

	/**
	 * Creates a new TransferCancelledException object.
	 */
	public TransferCancelledException() {
		super();
	}
}
