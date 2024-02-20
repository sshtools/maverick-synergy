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

import java.nio.ByteBuffer;

import com.sshtools.common.ssh.SshException;

/**
 * This interface provides a callback for writing SSH messages into the outgoing
 * buffer. When a message is to be sent you should call
 * {@link TransportProtocol#sendMessage(SshMessage)} with an implementation of
 * this interface. When the socket is ready for writing and your message is the
 * next available in the outgoing queue your implementation of
 * {@link #writeMessageIntoBuffer(ByteBuffer)} will be called. You should write
 * the SSH message into the ByteBuffer and return. When the message has been
 * written out to the socket {@link #messageSent()} will be called.<br>
 * <br>
 * The following code demonstrates how the interface is used by the transport
 * protocol to send the SSH_MSG_NEWKEYS. <blockquote>
 * 
 * <pre>
 * sendMessage(new SshMessage() {
 *     public void writeMessageIntoBuffer(ByteBuffer buf) {
 *         buf.put((byte) TransportProtocol.SSH_MSG_NEWKEYS);
 *     }
 * 
 *     public void messageSent(Long sequenceNo) {
 *         // Potentially we could generate the keys if we have received SSH_MSG_NEWKEYS
 *         synchronized (keyExchange) {
 *             if(Log.isDebugEnabled()) Log.debug(&quot;Sent SSH_MSG_NEWKEYS&quot;);
 *             keyExchange.sentNewKeys = true;
 *             generateNewKeys();
 *         }
 *     }
 * });
 * 
 * </pre>
 * 
 * </blockquote>
 * 
 * <p>
 * <em>For most server implementations it will not be required to implement
 * this interface. It is essentially for internal use but may be used if you
 * develop a custom authentication mechanism that requires additional messages
 * to be sent to the client</em>
 * </p>
 * 
 * @author Lee David Painter
 */
public interface SshMessage {

    /**
     * Write the SSH message data into a ByteBuffer.
     * 
     * @param buf
     * @return <tt>true</tt> if no more messages of this type are to be sent,
     *         otherwise return <tt>false</tt> to remain the first in the
     *         queue to send more messages of this type.
     */
    public boolean writeMessageIntoBuffer(ByteBuffer buf);

    public void messageSent(Long sequenceNo) throws SshException;
}
