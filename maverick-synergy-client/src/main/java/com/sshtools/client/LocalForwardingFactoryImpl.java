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
package com.sshtools.client;

import java.nio.channels.SocketChannel;

import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.common.ssh.ForwardingChannel;
import com.sshtools.synergy.common.ssh.LocalForwardingChannel;
import com.sshtools.synergy.common.ssh.SocketListeningForwardingFactoryImpl;

/**
 *  Implements the configuration of a local forwarding listening socket.
 */
public class LocalForwardingFactoryImpl extends
		SocketListeningForwardingFactoryImpl<SshClientContext> {

	String hostToConnect;
	int portToConnect;

	public LocalForwardingFactoryImpl(String hostToConnect, int portToConnect) {
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}
	
	@Override
	public String getChannelType() {
		return LocalForwardingChannel.LOCAL_FORWARDING_CHANNEL_TYPE;
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
	protected ForwardingChannel<SshClientContext> createChannel(String channelType,
			SshConnection con, 
			String addressToBind, int portToBind, SocketChannel sc, SshClientContext context) {
		return new LocalForwardingChannel<SshClientContext>(getChannelType(), con, hostToConnect, portToConnect, sc);
	}

}
