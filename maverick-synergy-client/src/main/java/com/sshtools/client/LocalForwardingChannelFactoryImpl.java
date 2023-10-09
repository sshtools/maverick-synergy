package com.sshtools.client;

import java.nio.channels.SocketChannel;

import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.ForwardingChannel;
import com.sshtools.synergy.ssh.LocalForwardingChannel;
import com.sshtools.synergy.ssh.SocketListeningForwardingChannelFactoryImpl;

/**
 *  Implements the configuration of a local forwarding listening socket.
 */
public class LocalForwardingChannelFactoryImpl extends
		SocketListeningForwardingChannelFactoryImpl<SshClientContext> {

	String hostToConnect;
	int portToConnect;

	public LocalForwardingChannelFactoryImpl(String hostToConnect, int portToConnect) {
		this.hostToConnect = hostToConnect;
		this.portToConnect = portToConnect;
	}
	
	@Override
	public String getChannelType() {
		return LocalForwardingChannel.LOCAL_FORWARDING_CHANNEL_TYPE;
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
	protected ForwardingChannel<SshClientContext> createChannel(String channelType,
			SshConnection con, 
			String addressToBind, int portToBind, SocketChannel sc, SshClientContext context) {
		return new LocalForwardingChannel<SshClientContext>(getChannelType(), con, hostToConnect, portToConnect, sc);
	}

}
