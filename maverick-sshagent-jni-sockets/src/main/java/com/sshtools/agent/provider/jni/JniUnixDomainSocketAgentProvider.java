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
package com.sshtools.agent.provider.jni;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;
import com.sshtools.agent.server.ServerSocketTransportAdapter;

public class JniUnixDomainSocketAgentProvider implements AgentProvider {

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			Socket socket = AFUNIXSocket.newInstance();
			File socketFile = new File(location);
			socket.connect(new AFUNIXSocketAddress(socketFile));
			return new SshAgentClient(false, application, socket, socket.getInputStream(), socket.getOutputStream(),
					false);
		}
		
		return null;
	}

	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			AFUNIXServerSocket server = AFUNIXServerSocket.newInstance(); 
			server.bind(new AFUNIXSocketAddress(new File(location)));
			return new ServerSocketTransportAdapter(server);
		}
		return null;
	}

}
