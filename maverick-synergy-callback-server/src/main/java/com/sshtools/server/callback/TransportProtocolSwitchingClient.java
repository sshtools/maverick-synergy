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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.callback;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.TransportProtocolClient;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.TransportProtocolServer;
import com.sshtools.synergy.common.nio.ConnectRequestFuture;
import com.sshtools.synergy.common.nio.LicenseException;
import com.sshtools.synergy.common.nio.ProtocolContextFactory;

class TransportProtocolSwitchingClient extends TransportProtocolClient {

	ProtocolContextFactory<SshServerContext> serverFactory;
	String callbackIdentifier;
	ConnectRequestFuture connectFuture;
	
	public TransportProtocolSwitchingClient(SshClientContext sshContext, 
			String callbackIdentifier,
			ProtocolContextFactory<SshServerContext> serverFactory,
			ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext, connectFuture);
		this.serverFactory = serverFactory;
		this.callbackIdentifier = callbackIdentifier;
		this.connectFuture = connectFuture;
	}

	@Override
	protected void onRemoteIdentificationReceived(String remoteIdentification) {
		
		if(!remoteIdentification.startsWith("SSH-2.0-" + callbackIdentifier)) {
			try {
				SshServerContext context = serverFactory.createContext(sshContext.getDaemonContext(), getSocketConnection().getSocketChannel());
				TransportProtocolServer engine = (TransportProtocolServer) context.createEngine(connectFuture);
				
				transferState(engine);
				getSocketConnection().setProtocolEngine(engine);
				
			} catch (Exception e) {
				disconnect(PROTOCOL_ERROR, "Failed to switch SSH role");
			}
		} else {
			/**
			 * We need to know the username to initiate authentication since we are acting as the client. So
			 * the callback client places the username in the initial SSH identification string.
			 */
			String username = remoteIdentification.substring(8+callbackIdentifier.length()).trim();
			getContext().setUsername(username);
		}
		
	}

	
}
