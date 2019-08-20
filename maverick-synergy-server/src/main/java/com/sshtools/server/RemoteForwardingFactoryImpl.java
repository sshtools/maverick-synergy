/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server;

import java.nio.channels.SocketChannel;

import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.ForwardingChannel;
import com.sshtools.common.ssh.LocalForwardingChannel;
import com.sshtools.common.ssh.RemoteForwardingChannel;
import com.sshtools.common.ssh.SocketListeningForwardingFactoryImpl;
import com.sshtools.common.ssh.SshConnection;

public class RemoteForwardingFactoryImpl extends
		SocketListeningForwardingFactoryImpl<SshServerContext> {

	public static final RemoteForwardingFactoryImpl INSTANCE = new RemoteForwardingFactoryImpl();
	
	
	@Override
	public String getChannelType() {
		return LocalForwardingChannel.REMOTE_FORWARDING_CHANNEL_TYPE;
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
		return new RemoteForwardingChannel<SshServerContext>(con, addressToBind, portToBind, sc, context);
	}

}
