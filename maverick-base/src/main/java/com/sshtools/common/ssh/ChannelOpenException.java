/* HEADER */
package com.sshtools.common.ssh;

/**
 * Exception thrown when a channel cannot be opened, the reason for which should
 * be specified in with any of the constants defined here.
 * 
 * @author Lee David Painter
 */
public class ChannelOpenException extends Exception {

	private static final long serialVersionUID = 6894031873211048989L;

	/** The administrator does not permit this channel to be opened **/
	public static final int ADMINISTRATIVIVELY_PROHIBITED = 1;

	/** The connection could not be established **/
	public static final int CONNECT_FAILED = 2;

	/** The channel type is unknown **/
	public static final int UNKNOWN_CHANNEL_TYPE = 3;

	/** There are no more resources available to open the channel **/
	public static final int RESOURCE_SHORTAGE = 4;

	int reason;

	public ChannelOpenException(String msg, int reason) {
		super(msg);
		this.reason = reason;
	}

	public int getReason() {
		return reason;
	}
}
