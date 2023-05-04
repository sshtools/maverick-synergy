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
		if (channeltype.equals(UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST)) {
			return new UnixDomainSocketRemoteForwardingChannel<SshServerContext>(
					UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST, con, hostToConnect, 0, null,
					(SshServerContext) con.getContext());
		}
		return super.onCreateChannel(channeltype, con);
	}
}
