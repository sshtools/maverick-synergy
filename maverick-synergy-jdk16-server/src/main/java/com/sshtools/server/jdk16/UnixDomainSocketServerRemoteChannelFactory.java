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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.jdk16.UnixDomainSocketRemoteForwardingChannel;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ChannelNG;

public class UnixDomainSocketServerRemoteChannelFactory extends DefaultServerChannelFactory {

	private String hostToConnect;

	public UnixDomainSocketServerRemoteChannelFactory(String hostToConnect) {
		this.hostToConnect = hostToConnect;
	}

	protected ChannelNG<SshServerContext> onCreateChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {
		if (channeltype.equals(UnixDomainSockets.STREAM_LOCAL_FORWARD_CHANNEL)) {
			return new UnixDomainSocketRemoteForwardingChannel<SshServerContext>(
					UnixDomainSockets.STREAM_LOCAL_FORWARD_CHANNEL, con, hostToConnect, 0, null,
					(SshServerContext) con.getContext());
		}
		return super.onCreateChannel(channeltype, con);
	}
}
