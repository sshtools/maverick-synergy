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
/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Desktop SSH Agent.
 *
 * Desktop SSH Agent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Desktop SSH Agent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Desktop SSH Agent.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.agent.provider.namedpipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.agent.server.SshAgentAcceptor;
import com.sshtools.agent.server.SshAgentTransport;

public class NamedPipeAcceptor implements SshAgentAcceptor {

	NamedPipeServer namedPipe;
	
	public NamedPipeAcceptor(NamedPipeServer namedPipe) {
		this.namedPipe = namedPipe;
	}
	
	@Override
	public SshAgentTransport accept() throws IOException {
		NamedPipeServer.NamedPipeSession session = namedPipe.accept();
		if(session==null) {
			return null;
		}
		return new NamedPipeTransport(session);
	}

	@Override
	public void close() throws IOException {
		namedPipe.close();
	}

	class NamedPipeTransport implements SshAgentTransport {

		NamedPipeServer.NamedPipeSession session;
		
		NamedPipeTransport(NamedPipeServer.NamedPipeSession session) {
			this.session = session;
		}
		
		@Override
		public void close() throws IOException {
			session.close();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return session.getInputStream();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return session.getOutputStream();
		}
		
	}
}
