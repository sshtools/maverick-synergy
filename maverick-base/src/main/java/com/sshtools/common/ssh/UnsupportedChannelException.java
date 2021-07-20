
package com.sshtools.common.ssh;

public class UnsupportedChannelException extends Exception {

	private static final long serialVersionUID = 4309055444197576779L;

	public UnsupportedChannelException() {
	}

	public UnsupportedChannelException(String message) {
		super(message);
	}

	public UnsupportedChannelException(Throwable cause) {
		super(cause);
	}

	public UnsupportedChannelException(String message, Throwable cause) {
		super(message, cause);
	}

}
