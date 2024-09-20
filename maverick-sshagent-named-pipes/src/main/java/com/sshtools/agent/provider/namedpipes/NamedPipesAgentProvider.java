package com.sshtools.agent.provider.namedpipes;

/*-
 * #%L
 * Named Pipes Key Agent Provider
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

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;

public class NamedPipesAgentProvider implements AgentProvider {

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if(type == AgentSocketType.WINDOWS_NAMED_PIPE) {
			NamedPipeClient namedPipe = new NamedPipeClient(location);
			return new SshAgentClient(false, application, namedPipe, namedPipe.getInputStream(), namedPipe.getOutputStream(), false);
		}
		return null;
	}

	@SuppressWarnings("resource")
	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.WINDOWS_NAMED_PIPE) {
			return new NamedPipeAcceptor(new NamedPipeServer(location));
		}
		return null;
	}

}
