
package com.sshtools.agent.exceptions;

import com.sshtools.common.ssh.SshException;

public class InvalidMessageException extends SshException {

	private static final long serialVersionUID = -5307916551875123863L;

	/**
     * <p>
     * Constructs the message.
     * </p>
     *
     * @param msg the error description
     *
     * @since 0.2.0
     */
    public InvalidMessageException(String msg,int reason) {
        super(msg,reason);
    }
}
