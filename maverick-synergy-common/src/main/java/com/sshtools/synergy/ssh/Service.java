/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sshtools.synergy.ssh;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;

/**
 * A service is a protocol that operates on top of the {@link TransportProtocol}.
 */
public interface Service {

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
     * @throws SshException 
     */
    public boolean processMessage(byte[] msg) throws IOException, SshException;

    /**
     * Start the service.
     * @throws SshException 
     */
    public void start() throws SshException;

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
	 * The service name
	 * @return
	 */
	public String getName();

	/**
	 * The service has reached idle timeout seconds
	 * @return 
	 */
	public boolean idle();
}
