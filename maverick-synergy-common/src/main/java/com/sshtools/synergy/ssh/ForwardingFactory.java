package com.sshtools.synergy.ssh;

public interface ForwardingFactory<C extends SshContext, F extends ForwardingChannelFactory<C>> {

	F createChannelFactory(String hostToConnect, int portToConnect);
}
