/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.callback;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.SshServerContext;

/**
 * An abstract server that provides a callback facility, listening on a port and acting as a client to 
 * any callback clients that connect to it. The callback client similarly acts as a server allowing 
 * this server to perform operations on the remote client.
 * 
 * The server also has the facility to act as a normal server. Switching modes depending on the 
 * client identifier provided by the SSH client.
 */
public abstract class CallbackServer extends SshEngine {

	String callbackIdentifier;
	
	protected CallbackServer(String callbackIdentifier) throws IOException {
		this.callbackIdentifier = callbackIdentifier;
	}
	
	public void addInterface(InetAddress addressToBind, int portToBind) throws IOException {
		getContext().addListeningInterface(addressToBind, portToBind, new ClientContextFactory(), true);
	}
	
	protected abstract void configureClientContext(SshClientContext clientContext);

	protected abstract SshServerContext createServerContext(SshEngineContext daemonContext, 
			SocketChannel sc);
	

	class ClientContextFactory implements ProtocolContextFactory<SshClientContext> {

		@Override
		public SshClientContext createContext(SshEngineContext daemonContext, SocketChannel sc)
				throws IOException, SshException {
			SshClientContext clientContext = new SwitchingSshContext(CallbackServer.this, callbackIdentifier, new ServerContextFactory());
			configureClientContext(clientContext);
			return clientContext;
		}
		
	}
	
	class ServerContextFactory implements ProtocolContextFactory<SshServerContext> {

		@Override
		public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
				throws IOException, SshException {
			SshServerContext serverContext = createServerContext(daemonContext, sc);
			return serverContext;
		}
		
		
	}
}
