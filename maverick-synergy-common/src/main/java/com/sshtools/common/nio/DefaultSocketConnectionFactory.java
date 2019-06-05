package com.sshtools.common.nio;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Create a default SocketConnection.
 */
public class DefaultSocketConnectionFactory implements SocketConnectionFactory {

	@Override
	public SocketConnection createSocketConnection(SshEngineContext context, SocketAddress localAddress,
			SocketAddress remoteAddress) throws IOException {
		return new SocketConnection();
	}

}
