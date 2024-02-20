package com.sshtools.common.ssh;

/**
 * <p>
 * Generic exception for J2SSH Maverick exception handling. When an exception is
 * thrown a reason is attached to the exception so that the developer can
 * determine if its possible to proceed with the connection.
 * </p>
 * 
 * <p>
 * This
 * 
 * @author Lee David Painter
 */
public class SshException extends Exception {

	private static final long serialVersionUID = 9007933160589824147L;

	/**
	 * The connection unexpectedly terminated and so the connection can no
	 * longer be used. The exception message will contain the message from the
	 * exception that caused the termination.
	 */
	public static final int UNEXPECTED_TERMINATION = 1;

	/**
	 * The remote host disconnected following the normal SSH protocol
	 * disconnection procedure. The exception message will contain the message
	 * received from the remote host that describes the reason for the
	 * disconnection
	 */
	public static final int REMOTE_HOST_DISCONNECTED = 2;

	/**
	 * The SSH protocol was violated in some way by the remote host and the
	 * connection has been terminated. The exception message will contain a
	 * description of the protocol violation.
	 */
	public static final int PROTOCOL_VIOLATION = 3;

	/**
	 * The API has encountered an error because of incorrect usage. The state of
	 * the connection upon receiving this exception is unknown.
	 */
	public static final int BAD_API_USAGE = 4;

	/**
	 * An internal error occurred within the API; in all cases contact
	 * sshtools.com support with the details of this error and the state of the
	 * connection when receiving this exception is unknown.
	 */
	public static final int INTERNAL_ERROR = 5;

	/**
	 * Indicates that a channel has failed; this is used by channel
	 * implementations (such as port forwarding or session channels) to indicate
	 * that the channel has critically failed. Upon receiving this exception you
	 * should check the connection state to determine whether its still possible
	 * to use the connection.
	 */
	public static final int CHANNEL_FAILURE = 6;

	/**
	 * In setting up a context an algorithm was specified that is not supported
	 * by the API.
	 */
	public static final int UNSUPPORTED_ALGORITHM = 7;

	/**
	 * The user cancelled the connection.
	 */
	public static final int CANCELLED_CONNECTION = 8;

	/**
	 * The protocol failed to negotiate a transport algorithm or failed to
	 * verify the host key of the remote host.
	 */
	public static final int KEY_EXCHANGE_FAILED = 9;

	/**
	 * The connection could not be established.
	 */
	public static final int CONNECT_FAILED = 10;

	/**
	 * The API is not licensed!
	 */
	public static final int LICENSE_ERROR = 11;

	/**
	 * An attempt has been made to use a connection that has been closed and is
	 * no longer valid.
	 */
	public static final int CONNECTION_CLOSED = 12;

	/**
	 * An error has occurred within the agent.
	 */
	public static final int AGENT_ERROR = 13;

	/**
	 * An error has occurred the port forwarding system.
	 */
	public static final int FORWARDING_ERROR = 14;

	/**
	 * A request was made to allocate a pseudo terminal, but this request
	 * failed.
	 */
	public static final int PSEUDO_TTY_ERROR = 15;

	/**
	 * A request was made to start a shell, but this request failed.
	 */
	public static final int SHELL_ERROR = 15;

	/**
	 * An error occurred whilst accessing a sessions streams
	 */
	public static final int SESSION_STREAM_ERROR = 15;

	/**
	 * An error occurred in the JCE; typically this would result from Maverick
	 * attempting to use an algorithm that the JCE does not support.
	 */
	public static final int JCE_ERROR = 16;

	/**
	 * An error occurred reading the contents of a file. Its possible that the
	 * file is not correctly formatted.
	 */
	public static final int POSSIBLE_CORRUPT_FILE = 17;

	/**
	 * The user cancelled an active SCP transfer.
	 */
	public static final int SCP_TRANSFER_CANCELLED = 18;

	/**
	 * The API detected a socket timeout
	 */
	public static final int SOCKET_TIMEOUT = 19;

	/**
	 * The Shell class failed to detect the prompt.
	 */
	public static final int PROMPT_TIMEOUT = 20;

	/**
	 * An expected message was not received before the specified timeout period.
	 */
	public static final int MESSAGE_TIMEOUT = 21;

	/**
	 * API error to indicate a host key signature failed.
	 */
	public static final int HOST_KEY_ERROR = 0xF000;

	/**
	 * An operation was not supported
	 */
	public static final int UNSUPPORTED_OPERATION = 0xE007;
	
	int reason;
	Throwable cause;

	/**
	 * Create an exception with the given description and reason.
	 * 
	 * @param msg
	 * @param reason
	 */
	public SshException(String msg, int reason) {
		this(msg, reason, null);
	}
	
	/**
	 * Create an exception with the given description and reason (for compatibility with Legacy API).
	 * 
	 * @param msg
	 * @param reason
	 */
	public SshException(int reason, String msg) {
		this(msg, reason, null);
	}

	/**
	 * Create an exception with the given cause and reason.
	 * 
	 * @param reason
	 * @param cause
	 */
	public SshException(int reason, Throwable cause) {
		this(null, reason, cause);
	}

	public SshException(Throwable cause, int reason) {
		this(null, reason, cause);
	}

	/**
	 * Create an exception with the given description and cause. The reason
	 * given will be <code>INTERNAL_ERROR</code>.
	 * 
	 * @param msg
	 * @param cause
	 */
	public SshException(String msg, Throwable cause) {
		this(msg, INTERNAL_ERROR, cause);
	}

	/**
	 * Create an exception by providing the cause of the error. This constructor
	 * sets the reason to INTERNAL_ERROR.
	 * 
	 * @param cause
	 */
	public SshException(Throwable cause) {
		this(cause.getMessage(), cause);
	}

	/**
	 * Create an exception with the given description cause, reason.
	 * 
	 * @param msg
	 * @param reason
	 * @param cause
	 */
	public SshException(String msg, int reason, Throwable cause) {
		super(msg == null ? (cause == null ? "Unknown cause" : cause.getClass()
				.getName())
				: msg);
		this.cause = cause;
		this.reason = reason;
	}

	/**
	 * Get the reason for the exception
	 * 
	 * @return int
	 */
	public int getReason() {
		return reason;
	}

	/**
	 * If an INTERNAL_ERROR reason is given this method MAY return the cause of
	 * the error.
	 * 
	 * @return Throwable
	 */
	public Throwable getCause() {
		return cause;
	}

}
