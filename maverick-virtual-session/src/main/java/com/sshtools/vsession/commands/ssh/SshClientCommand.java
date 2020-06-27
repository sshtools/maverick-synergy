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
package com.sshtools.vsession.commands.ssh;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.AbstractCommandTask;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.commands.sftp.SftpClientOptions;

public class SshClientCommand extends AbstractSshClientCommand {

	public SshClientCommand() {
		super("ssh", SUBSYSTEM_SHELL, "", "Returns the ssh client shell");
		for (Option option : SftpClientOptions.getOptions()) {
			this.options.addOption(option);
		}
	}
	
	@Override
	protected void runCommand(SshClient sshClient, SshClientArguments arguments, VirtualConsole console) {

		if (CommandUtil.isNotEmpty(arguments.getCommand())) {
			String command = arguments.getCommand();
			Connection<SshClientContext> connection = sshClient.getConnection();
			AbstractCommandTask task = new AbstractCommandTask(connection, command) {
				
				@Override
				protected void beforeExecuteCommand(SessionChannelNG session) {
					session.allocatePseudoTerminal(console.getTerminal().getType(), console.getTerminal().getWidth(),
							console.getTerminal().getHeight());
				}
				
				@Override
				protected void onOpenSession(SessionChannelNG session) throws IOException {
					
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
			
			connection.addTask(task);
			task.waitForever();
			console.getSessionChannel().disableRawMode();

			return;
		}

		Connection<SshClientContext> connection = sshClient.getConnection();

		ShellTask shell = new ShellTask(connection) {

			protected void beforeStartShell(SessionChannelNG session) {

				session.allocatePseudoTerminal(console.getTerminal().getType(), console.getTerminal().getWidth(),
						console.getTerminal().getHeight());
			}

			@Override
			protected void onOpenSession(final SessionChannelNG session)
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

		connection.addTask(shell);
		shell.waitForever();

		console.getSessionChannel().disableRawMode();
		console.println();

	}


	@Override
	protected SshClientArguments generateCommandArguments(CommandLine cli, String[] args) {
		return SshClientOptionsEvaluator.evaluate(cli, args);
	}

}
