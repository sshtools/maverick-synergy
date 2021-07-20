
package com.sshtools.synergy.nio;

import java.io.IOException;

/**
 * Thrown by the licensing system if there is a problem with the license.
 */
public class LicenseException extends IOException {

	private static final long serialVersionUID = -8522772811443440207L;

	public LicenseException(String msg) {
		super(msg);
	}
}
