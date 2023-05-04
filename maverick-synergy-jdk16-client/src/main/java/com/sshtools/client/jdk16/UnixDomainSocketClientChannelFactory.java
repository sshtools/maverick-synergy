package com.sshtools.client.jdk16;

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
