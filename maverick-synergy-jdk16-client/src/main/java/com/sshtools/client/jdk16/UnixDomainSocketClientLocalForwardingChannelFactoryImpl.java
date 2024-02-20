package com.sshtools.client.jdk16;

/*-
 * #%L
 * Client implementation Unix Domain Socket forwarding
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

import java.nio.channels.SocketChannel;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.jdk16.UnixDomainSocketForwardingChannelFactory;
import com.sshtools.synergy.jdk16.UnixDomainSocketLocalForwardingChannel;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ForwardingChannel;

public class UnixDomainSocketClientLocalForwardingChannelFactoryImpl
		extends UnixDomainSocketForwardingChannelFactory<SshClientContext> {

	String hostToConnect;

	public UnixDomainSocketClientLocalForwardingChannelFactoryImpl(String hostToConnect) {
		this.hostToConnect = hostToConnect;
	}

	@Override
	public String getChannelType() {
		return UnixDomainSockets.DIRECT_STREAM_LOCAL_CHANNEL;
	}

	@Override
	public int getStartedEventCode() {
		return EventCodes.EVENT_FORWARDING_LOCAL_STARTED;
	}

	@Override
	public int getStoppedEventCode() {
		return EventCodes.EVENT_FORWARDING_LOCAL_STOPPED;
	}

	@Override
	protected ForwardingChannel<SshClientContext> createChannel(String channelType, SshConnection con,
			String addressToBind, int portToBind, SocketChannel sc, SshClientContext context) {
		return new UnixDomainSocketLocalForwardingChannel<SshClientContext>(getChannelType(), con, hostToConnect, sc);
	}
}
