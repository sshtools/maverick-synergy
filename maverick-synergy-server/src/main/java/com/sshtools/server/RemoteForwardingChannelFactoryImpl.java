package com.sshtools.server;

import java.nio.channels.SocketChannel;

import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.ForwardingChannel;
import com.sshtools.synergy.ssh.LocalForwardingChannel;
import com.sshtools.synergy.ssh.RemoteForwardingChannel;
import com.sshtools.synergy.ssh.SocketListeningForwardingChannelFactoryImpl;

public class RemoteForwardingChannelFactoryImpl extends
		SocketListeningForwardingChannelFactoryImpl<SshServerContext> {

	public static final RemoteForwardingChannelFactoryImpl INSTANCE = new RemoteForwardingChannelFactoryImpl();
	
	
	@Override
	public String getChannelType() {
		return LocalForwardingChannel.REMOTE_FORWARDING_CHANNEL_TYPE;
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
		return new RemoteForwardingChannel<SshServerContext>(con, addressToBind, portToBind, sc);
	}

}
