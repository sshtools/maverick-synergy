/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
