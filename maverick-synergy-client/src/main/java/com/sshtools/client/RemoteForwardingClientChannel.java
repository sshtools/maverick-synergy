package com.sshtools.client;

import com.sshtools.common.ssh.RemoteForwardingChannel;
import com.sshtools.common.ssh.SshConnection;

/**
 * Concrete implementation of a remote forwarding channel for the client implementation.
 */
public class RemoteForwardingClientChannel extends RemoteForwardingChannel<SshClientContext> {

	public RemoteForwardingClientChannel(SshConnection con, SshClientContext context) {
		super(con, context);
	}

}
