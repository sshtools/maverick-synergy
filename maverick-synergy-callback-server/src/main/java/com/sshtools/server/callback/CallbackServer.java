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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.AbstractSshServer;
import com.sshtools.synergy.nio.ProtocolContextFactory;

/**
 * An abstract server that provides a callback facility, listening on a port and acting as a client to 
 * any callback clients that connect to it. The callback client similarly acts as a server allowing 
 * this server to perform operations on the remote client.
 * 
 * The server also has the facility to act as a normal server. Switching modes depending on the 
 * client identifier provided by the SSH client.
 */
public class CallbackServer extends AbstractSshServer {

	
	CallbackContextFactory defaultContextFactory;
	
	public CallbackServer(CallbackContextFactory callbackContextFactory) {
		this.defaultContextFactory = callbackContextFactory;
	}
	
	public CallbackServer(CallbackContextFactory callbackContextFactory, InetAddress addressToBind, int port) {
		super(addressToBind, port);
		this.defaultContextFactory = callbackContextFactory;
	}
	
	public CallbackServer(CallbackContextFactory callbackContextFactory, int port) throws UnknownHostException {
		super(port);
		this.defaultContextFactory = callbackContextFactory;
	}

	public CallbackServer(CallbackContextFactory callbackContextFactory, String addressToBind, int port) throws UnknownHostException {
		super(addressToBind, port);
		this.defaultContextFactory = callbackContextFactory;
	}

	@Override
	protected ProtocolContextFactory<?> getDefaultContextFactory() {
		return defaultContextFactory;
	}

	public SshConnection getCallbackClient(String hostToConnect) {
		return defaultContextFactory.getCallbackClient(hostToConnect);
	}

	public Collection<SshConnection> getCallbackClients() {
		return defaultContextFactory.getCallbackClients();
	}

}
