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
