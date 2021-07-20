
package com.sshtools.synergy.nio;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Interface for creating SocketConnection objects.
 */
public interface SocketConnectionFactory {
	SocketConnection createSocketConnection(SshEngineContext context, SocketAddress localAddress,
			SocketAddress remoteAddress) throws IOException;
}
