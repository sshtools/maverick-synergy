package com.sshtools.server.vsession;

public class UnsupportedCommandException extends Exception {

	private static final long serialVersionUID = 4922508217776899504L;

	public UnsupportedCommandException(String msg) {
		super(msg);
	}
}
