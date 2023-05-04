/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.IOException;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.ProtocolEngine;
import com.sshtools.synergy.nio.SshEngine;

public class SwitchingSshContext extends SshClientContext {

	ProtocolContextFactory<SshServerContext> serverFactory;
	String clientIdentifier;
	
	public SwitchingSshContext(SshEngine engine, 
			String clientIdentifier,
			ProtocolContextFactory<SshServerContext> serverFactory) throws IOException, SshException {
		super(engine);
		this.clientIdentifier = clientIdentifier;
		this.serverFactory = serverFactory;
		setSoftwareVersionComments("CallbackServer");
	}
	
	public SwitchingSshContext(SshEngine engine, 
			String clientIdentifier,
			ProtocolContextFactory<SshServerContext> serverFactory,
			SecurityLevel securityLevel) throws IOException, SshException {
		super(engine, securityLevel);
		this.clientIdentifier = clientIdentifier;
		this.serverFactory = serverFactory;
		setSoftwareVersionComments("CallbackServer");
	}

	@Override
	public ProtocolEngine createEngine(ConnectRequestFuture connectFuture) throws IOException {
		return transport = new TransportProtocolSwitchingClient(this, clientIdentifier, serverFactory, connectFuture);
	}

	
}
