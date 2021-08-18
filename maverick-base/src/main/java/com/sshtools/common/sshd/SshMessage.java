/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */


package com.sshtools.common.sshd;

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
