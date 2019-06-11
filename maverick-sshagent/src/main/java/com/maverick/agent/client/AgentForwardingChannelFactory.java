package com.maverick.agent.client;

import java.io.IOException;

import com.maverick.agent.exceptions.AgentNotAvailableException;
import com.maverick.ssh2.ChannelFactory;
import com.maverick.ssh2.Ssh2Channel;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;

public class AgentForwardingChannelFactory implements ChannelFactory {
	
	String location;
	AgentSocketType socketType;
	
	public AgentForwardingChannelFactory(String location, AgentSocketType socketType) {
		this.location = location;
		this.socketType = socketType;
	}
	
	public String[] supportedChannelTypes() {
		return new String[] { "auth-agent", "auth-agent@openssh.com"};
	}
	
	public Ssh2Channel createChannel(String channeltype, byte[] requestdata) throws SshException, ChannelOpenException {
		try {
			return new AgentSocketForwardingChannel(channeltype,
					SshAgentClient.connectAgentSocket(location, socketType));
		} catch (AgentNotAvailableException e) {
			throw new ChannelOpenException("", 0);
		} catch(IOException e) {
			throw new ChannelOpenException("", 0);
		}
	}
}
