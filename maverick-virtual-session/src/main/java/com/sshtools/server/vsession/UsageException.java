
package com.sshtools.server.vsession;

public class UsageException extends Exception {

	private static final long serialVersionUID = 1157003532529383610L;

	public UsageException(String usage) {
		super(usage);
	}
}
