package com.sshtools.server.jdk16;

import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.ssh.ForwardingChannelFactory;
import com.sshtools.synergy.ssh.ForwardingFactory;

public final class UnixDomainSocketServerRemoteForwardingFactory
		implements ForwardingFactory<SshServerContext, ForwardingChannelFactory<SshServerContext>> {
	@Override
	public ForwardingChannelFactory<SshServerContext> createChannelFactory(String hostToConnect, int portToConnect) {
		return new UnixDomainSocketServerRemoteForwardingChannelFactoryImpl();
	}
}