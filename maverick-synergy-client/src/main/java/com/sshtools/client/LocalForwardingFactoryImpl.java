package com.sshtools.client;

import java.nio.channels.SocketChannel;

import com.sshtools.common.events.EventCodes;
import com.sshtools.common.ssh.ForwardingChannel;
import com.sshtools.common.ssh.LocalForwardingChannel;
import com.sshtools.common.ssh.SocketListeningForwardingFactoryImpl;
import com.sshtools.common.ssh.SshConnection;

/**
 *  Implements the configuration of a local forwarding listening socket.
 */
public class LocalForwardingFactoryImpl extends
		SocketListeningForwardingFactoryImpl<SshClientContext> {

	String hostToConnect;
	int portToConnect;

	public LocalForwardingFactoryImpl(String hostToConnect, int portToConnect) {
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
		return new LocalForwardingChannel<SshClientContext>(getChannelType(), con, hostToConnect, portToConnect, sc, context);
	}

}
