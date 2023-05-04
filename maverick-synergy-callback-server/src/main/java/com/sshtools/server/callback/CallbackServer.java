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
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sshtools.common.auth.InMemoryMutualKeyAuthenticationStore;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.vfs.VFSFileFactory;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.server.AbstractSshServer;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.callback.commands.CallbackCommandFactory;
import com.sshtools.server.vsession.VirtualChannelFactory;
import com.sshtools.synergy.nio.ProtocolContextFactory;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.vsession.commands.ssh.SshClientsCommandFactory;

/**
 * An abstract server that provides a callback facility, listening on a port and acting as a client to 
 * any callback clients that connect to it. The callback client similarly acts as a server allowing 
 * this server to perform operations on the remote client.
 * 
 * The server also has the facility to act as a normal server. Switching modes depending on the 
 * client identifier provided by the SSH client.
 */
public class CallbackServer extends AbstractSshServer {

	
	InMemoryMutualKeyAuthenticationStore store = new InMemoryMutualKeyAuthenticationStore();
	InMemoryCallbackRegistrationService callbacks = new InMemoryCallbackRegistrationService();

	public CallbackServer() {
		super();
	}

	public CallbackServer(InetAddress addressToBind, int port) {
		super(addressToBind, port);
	}

	public CallbackServer(int port) throws UnknownHostException {
		super(port);
	}

	public CallbackServer(String addressToBind, int port) throws UnknownHostException {
		super(addressToBind, port);
	}

	@Override
	public ProtocolContextFactory<?> getDefaultContextFactory() {
		return new CallbackContextFactory(store, callbacks, this);
	}
	
	public FileFactory getFileFactory() {
		return new FileFactory() {

			@Override
			public AbstractFileFactory<?> getFileFactory(SshConnection con)
					throws IOException, PermissionDeniedException {
				return new VirtualFileFactory(new VirtualMountTemplate("/", 
						"ram://" + con.getUsername(),
						new VFSFileFactory(), true));
			}	
		};
	}
	
	@Override
	public ChannelFactory<SshServerContext> getChannelFactory() {
		return new VirtualChannelFactory(new CallbackCommandFactory(callbacks),
				new SshClientsCommandFactory());
	}
	
	public void addAgentKey(String username, SshKeyPair privateKey, SshPublicKey publicKey) {
		store.addKey(username, privateKey, publicKey);
	}

}
