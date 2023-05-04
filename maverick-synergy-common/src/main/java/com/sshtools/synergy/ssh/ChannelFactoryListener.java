package com.sshtools.synergy.ssh;

public interface ChannelFactoryListener<T extends SshContext> {

	default void onChannelCreated(ChannelNG<T> channel) {  }
}
