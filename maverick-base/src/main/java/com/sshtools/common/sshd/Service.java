package com.sshtools.common.sshd;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import com.sshtools.common.ssh.Context;

/**
 * A service is a protocol that operates on top of the {@link TransportProtocol}.
 * There is only one active service at anyone time and the current SSH protocol
 * defines the {@link AuthenticationProtocol} and the {@link ConnectionProtocol}.
 * 
 * @author Lee David Painter
 */
public interface Service<C extends Context> {

    /**
     * Initialize the service.
     * 
     * @param transport
     * @throws IOException
     */
    public void init(AbstractServerTransport<C> transport) throws IOException;

    /**
     * Process a transport message. When a message is received by the
     * {@link TransportProtocol} that is not a transport level message the
     * message is passed onto the active service using this method. The service
     * processes the message and returns a value to indicate whether the message
     * was used.
     * 
     * @param msg
     * @return <tt>true</tt> if the message was processed, otherwise
     *         <tt>false</tt>
     * @throws IOException
     */
    public boolean processMessage(byte[] msg) throws IOException;

    /**
     * Start the service.
     */
    public void start();

    /**
     * Stop the service
     */
    public void stop();

    /**
     * How long does the service allow idle for?
     * @return
     */
	public int getIdleTimeoutSeconds();

	/**
	 * Transport level idle
	 */
	public void idle();
}
