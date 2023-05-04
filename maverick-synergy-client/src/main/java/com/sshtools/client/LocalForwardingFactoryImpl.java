package com.sshtools.client;

import com.sshtools.synergy.ssh.ForwardingFactory;

public class LocalForwardingFactoryImpl<C extends SshClientContext> implements ForwardingFactory<SshClientContext, LocalForwardingChannelFactoryImpl> {

	public LocalForwardingFactoryImpl() {
	}

	@Override
	public LocalForwardingChannelFactoryImpl createChannelFactory(String hostToConnect, int portToConnect) {
		return new LocalForwardingChannelFactoryImpl(hostToConnect, portToConnect);
	}

}
