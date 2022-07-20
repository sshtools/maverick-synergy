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

package com.sshtools.client.jdk16;

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
