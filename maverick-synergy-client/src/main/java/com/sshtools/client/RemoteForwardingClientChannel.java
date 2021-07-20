
package com.sshtools.client;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.RemoteForwardingChannel;

/**
 * Concrete implementation of a remote forwarding channel for the client implementation.
 */
public class RemoteForwardingClientChannel extends RemoteForwardingChannel<SshClientContext> {

	public RemoteForwardingClientChannel(SshConnection con) {
		super(con);
	}

}
