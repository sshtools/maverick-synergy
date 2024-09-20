package com.sshtools.server;

/*-
 * #%L
 * Server API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.nio.SshEngineContext;

public class SshServer extends AbstractSshServer implements ProtocolContextFactory<SshServerContext> {

	public SshServer() {
		JCEComponentManager.getDefaultInstance();
	}
	
	public ProtocolContextFactory<?> getDefaultContextFactory() {
		return this;
	}

	public SshServer(int port) throws UnknownHostException {
		this("::", port);
	}
	
	public SshServer(String addressToBind, int port) throws UnknownHostException {
		this(InetAddress.getByName(addressToBind), port);
	}
	
	public SshServer(InetAddress addressToBind, int port) {
		this.addressToBind = addressToBind;
		this.port = port;
		JCEComponentManager.getDefaultInstance();
	}

	@Override
	public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
			throws IOException, SshException {
		return createServerContext(daemonContext, sc);
	}
	
	
}
