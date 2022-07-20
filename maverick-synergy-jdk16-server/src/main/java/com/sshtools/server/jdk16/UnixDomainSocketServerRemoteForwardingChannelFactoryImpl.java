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

package com.sshtools.server.jdk16;

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
