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
import java.util.Objects;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class CallbackShell extends CallbackCommand {

	public CallbackShell() {
		super("shell", "Callback", "shell <name>", "Open a shell to a named callback client");
	}
	
	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		if(args.length != 2) {
			throw new UsageException("Invalid number of arguments");
		}
		
		String clientName = args[1];
		SshConnection con = server.getCallbackClient(clientName);
		
		if(Objects.isNull(con)) {
			console.println(String.format("%s is not currently connected", clientName));
		}

		console.println(String.format("---- Opening shell on %s", clientName));
		console.println();
		
		ShellTask shell = new ShellTask(con) {

			protected void beforeStartShell(SessionChannelNG session) {
				
				session.allocatePseudoTerminal(console.getTerminal().getType(), 
						console.getTerminal().getWidth(), 
						console.getTerminal().getHeight());
			}
			
			@Override
			protected void onOpenSession(SessionChannelNG session)
					throws IOException, SshException, ShellTimeoutException {
				
				console.getSessionChannel().enableRawMode();
				
				con.addTask(new ConnectionAwareTask(con) {
					@Override
					protected void doTask() throws Throwable {
						IOUtils.copy(console.getSessionChannel().getInputStream(), session.getOutputStream());
					}
				});
				IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
			}
			
		};
		
		con.addTask(shell);
		shell.waitForever();
		
		console.getSessionChannel().disableRawMode();
		console.println();
		console.println(String.format("---- Exited shell on %s", clientName));
	}

}
