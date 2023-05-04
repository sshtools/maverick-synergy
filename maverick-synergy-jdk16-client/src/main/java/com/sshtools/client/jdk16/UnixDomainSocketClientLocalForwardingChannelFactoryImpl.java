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
