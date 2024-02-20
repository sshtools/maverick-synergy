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
		if (channeltype.equals(UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST)) {
			return new UnixDomainSocketRemoteForwardingChannel<SshServerContext>(
					UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST, con, hostToConnect, 0, null,
					(SshServerContext) con.getContext());
		}
		return super.onCreateChannel(channeltype, con);
	}
}
