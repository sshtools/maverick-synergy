package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
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

import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * <p>An abstract forwarding channel implementation for use with both local
 * and remote forwarding operations of both TCP and UDS types.</p>
 *
 * A forwarding channel acts as a tunnel, connections are listened for at the tunnel start point and any data is forwarded from the start point through the ssh connection and then onto the end point.
 * 
 *  Local forwards have the tunnel start point on the client, and the data flows from the start point through the client, along the ssh connection to the server, out to the endpoint which can be anywhere.
 *  Remote forwards have the tunnel start point on the Server, and the data flows from the start point through the server, along the ssh connection to the client, out to the endpoint which can be anywhere.
 */
public abstract class ForwardingChannel<T extends SshContext>
    extends ChannelNG<T> {

    /**
     * Construct the forwarding channel.
     * @param channelType String
     * @param maximumPacket int
     * @param windowSize int
     * @see com.sshtools.synergy.ssh.ChannelNG#Channel(String channelType, int maximumPacketSize, int initialWindowSize)
     */
    public ForwardingChannel(String channelType, int maximumPacketSize, UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace, UnsignedInteger32 minimumWindowSpace) {
        super(channelType, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace);
    }
    
    public ForwardingChannel(String channelType, int maximumPacketSize, UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace, UnsignedInteger32 minimumWindowSpace, boolean autoConsume) {
        super(channelType, maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, new ChannelRequestFuture(), autoConsume);
    }

    /**
     * The hostname of the endpoint of tunnel.
     * @return String
     * @deprecated cast to {@link TCPForwardingChannel} or {@link UnixDomainSocketForwardingChannel} if you need access to this.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    public String getHost() {
    	if(this instanceof TCPForwardingChannel tcpf)
    		return tcpf.getHost();
    	else if(this instanceof UnixDomainSocketForwardingChannel udsf)
       		return udsf.getPath();
    	else
    		throw new IllegalStateException("Unknown channel type.");
    }

    /**
     * The port number of the endpoint of tunnel.
     * @return int
     * @deprecated cast to {@link TCPForwardingChannel} or {@link UnixDomainSocketForwardingChannel} if you need access to this.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    public int getPort() {
    	if(this instanceof TCPForwardingChannel tcpf)
    		return tcpf.getPort();
    	else if(this instanceof UnixDomainSocketForwardingChannel)
       		return 0;
    	else
    		throw new IllegalStateException("Unknown channel type.");
    }

    /**
     * The hostname of the startpoint of tunnel.
     * @return String
     * @deprecated cast to {@link TCPForwardingChannel} if you need access to this.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    public String getOriginatingHost() {
    	if(this instanceof TCPForwardingChannel tcpf)
    		return tcpf.getOriginatingHost();
    	else
    		throw new IllegalStateException("Unknown channel type.");
    }
    
    /**
     * The port number of the startpoint of tunnel.
     * @return int
     * 
     * @deprecated cast to {@link TCPForwardingChannel} if you need access to this.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    public int getOriginatingPort() {
    	if(this instanceof TCPForwardingChannel tcpf)
    		return tcpf.getOriginatingPort();
    	else
    		throw new IllegalStateException("Unknown channel type.");
    }

}
