
package com.sshtools.common.ssh;

import java.io.IOException;

/**
 * This class is provided so that when a channel InputStream/OutputStream
 * interface has to throw an IOException; the real SshException cause can be
 * retrieved.
 * 
 * @author Lee David Painter
 */
public class SshIOException extends IOException {

	private static final long serialVersionUID = 6171680689279356698L;
	
	SshException realEx;

	/**
	 * Construct the exception with the real exception.
	 * 
	 * @param realEx
	 */
	public SshIOException(SshException realEx) {
		this.realEx = realEx;
	}

	/**
	 * Get the real exception
	 * 
	 * @return SshException
	 */
	public SshException getRealException() {
		return realEx;
	}
}
