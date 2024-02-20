package com.sshtools.server.jdk16;

/*-
 * #%L
 * Server implementation Unix Domain Socket forwarding
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

import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.jdk16.UnixDomainSocketForwardingChannelFactory;
import com.sshtools.synergy.jdk16.UnixDomainSocketRemoteForwardingChannel;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ForwardingChannel;

public class UnixDomainSocketServerRemoteForwardingChannelFactoryImpl extends
	UnixDomainSocketForwardingChannelFactory<SshServerContext> {

	public static final UnixDomainSocketServerRemoteForwardingChannelFactoryImpl INSTANCE = new UnixDomainSocketServerRemoteForwardingChannelFactoryImpl();
	
	
	@Override
	public String getChannelType() {
		return UnixDomainSockets.FORWARDED_STREAM_LOCAL_CHANNEL;
	}

	@Override
	public int getStartedEventCode() {
		return EventCodes.EVENT_FORWARDING_REMOTE_STARTED;
	}

	@Override
	public int getStoppedEventCode() {
		return EventCodes.EVENT_FORWARDING_REMOTE_STOPPED;
	}

	@Override
	protected ForwardingChannel<SshServerContext> createChannel(String channelType,
			SshConnection con, 
			String addressToBind, int portToBind, SocketChannel sc, SshServerContext context) {
		return new UnixDomainSocketRemoteForwardingChannel<SshServerContext>(channelType, con, addressToBind, portToBind, sc, context);
	}

}
