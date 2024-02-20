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

import com.sshtools.client.DefaultClientChannelFactory;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.synergy.jdk16.UnixDomainSocketLocalForwardingChannel;
import com.sshtools.synergy.jdk16.UnixDomainSocketRemoteForwardingChannel;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ChannelNG;

public class UnixDomainSocketClientChannelFactory extends DefaultClientChannelFactory {

	protected ChannelNG<SshClientContext> onCreateChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {
		if (channeltype.equals(UnixDomainSockets.DIRECT_STREAM_LOCAL_CHANNEL)) {
			return new UnixDomainSocketLocalForwardingChannel<SshClientContext>(
					channeltype, con);
		} else if (channeltype.equals(UnixDomainSockets.FORWARDED_STREAM_LOCAL_CHANNEL)) {
			return new UnixDomainSocketRemoteForwardingChannel<SshClientContext>(channeltype, con, null, 0, null,
					(SshClientContext) con.getContext());
		}
		return super.onCreateChannel(channeltype, con);
	}
}
