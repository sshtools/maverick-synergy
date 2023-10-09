package com.sshtools.server.jdk16;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.jdk16.UnixDomainSocketLocalForwardingChannel;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ChannelNG;

public class UnixDomainSocketServerLocalChannelFactory extends DefaultServerChannelFactory {

	protected ChannelNG<SshServerContext> onCreateChannel(String channeltype, SshConnection con)
			throws UnsupportedChannelException, PermissionDeniedException {
		if (channeltype.equals(UnixDomainSockets.DIRECT_STREAM_LOCAL_CHANNEL)) {
			return new UnixDomainSocketLocalForwardingChannel<SshServerContext>(
					UnixDomainSockets.DIRECT_STREAM_LOCAL_CHANNEL, con);
		}
		return super.onCreateChannel(channeltype, con);
	}
}
