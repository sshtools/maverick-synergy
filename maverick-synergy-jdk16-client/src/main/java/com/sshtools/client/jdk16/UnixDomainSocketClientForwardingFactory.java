package com.sshtools.client.jdk16;

import com.sshtools.client.SshClientContext;
import com.sshtools.synergy.ssh.ForwardingChannelFactory;
import com.sshtools.synergy.ssh.ForwardingFactory;

public final class UnixDomainSocketClientForwardingFactory
		implements ForwardingFactory<SshClientContext, ForwardingChannelFactory<SshClientContext>> {
	@Override
	public ForwardingChannelFactory<SshClientContext> createChannelFactory(String hostToConnect, int portToConnect) {
		return new UnixDomainSocketClientLocalForwardingChannelFactoryImpl(hostToConnect);
	}
}
