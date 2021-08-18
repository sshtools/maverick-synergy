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

package com.sshtools.agent.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class UnixSocketAdapter implements SshAgentAcceptor {

	ServerSocket socket;
	UnixSocketAdapter(ServerSocket socket) {
		this.socket = socket;
	}
	@Override
	public SshAgentTransport accept() throws IOException {
		Socket sock = socket.accept();
		if(sock==null) {
			return null;
		}
		return new UnixSocketTransport(sock);
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	class UnixSocketTransport implements SshAgentTransport {
		
		Socket sock;
		UnixSocketTransport(Socket sock) {
			this.sock = sock;
		}
		@Override
		public InputStream getInputStream() throws IOException {
			return sock.getInputStream();
		}
		@Override
		public OutputStream getOutputStream() throws IOException {
			return sock.getOutputStream();
		}
		@Override
		public void close() throws IOException {
			sock.close();
		}
	}
}
