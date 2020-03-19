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
package com.sshtools.server.callback.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.callback.CallbackServer;
import com.sshtools.server.vsession.CommandFactory;

public class CallbackCommandFactory extends CommandFactory<CallbackCommand> {

	CallbackServer server;
	
	public CallbackCommandFactory(CallbackServer server) {
		this.server = server;
		installShellCommands();
	}
	
	protected void installShellCommands() {
		installCommand(Callbacks.class);
		installCommand(CallbackShell.class);
		installCommand(CallbackMount.class);
	}

	@Override
	protected void configureCommand(CallbackCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		c.setServer(server);
		super.configureCommand(c, con);
	}

}
