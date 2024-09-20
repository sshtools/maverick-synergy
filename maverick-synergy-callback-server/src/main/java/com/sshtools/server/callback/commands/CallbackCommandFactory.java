package com.sshtools.server.callback.commands;

/*-
 * #%L
 * Callback Server API
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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.callback.CallbackRegistrationService;
import com.sshtools.server.vsession.CommandFactory;

public class CallbackCommandFactory extends CommandFactory<CallbackCommand> {

	CallbackRegistrationService server;
	
	public CallbackCommandFactory(CallbackRegistrationService server) {
		this.server = server;
		installShellCommands();
	}
	
	protected void installShellCommands() {
		installCommand(Callbacks.class);
		//installCommand(CallbackShell.class); // Use ssh command instead. 
		installCommand(CallbackMount.class);
	}

	@Override
	protected void configureCommand(CallbackCommand c, SshConnection con) throws IOException, PermissionDeniedException {
		c.setRegistrationService(server);
		super.configureCommand(c, con);
	}

}
