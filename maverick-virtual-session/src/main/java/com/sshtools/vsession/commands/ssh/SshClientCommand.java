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

package com.sshtools.vsession.commands.ssh;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.tasks.AbstractCommandTask;
import com.sshtools.client.tasks.AbstractSessionTask;
import com.sshtools.client.tasks.ShellTask.ShellTaskBuilder;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualShellNG;
import com.sshtools.server.vsession.VirtualShellNG.WindowSizeChangeListener;
import com.sshtools.server.vsession.commands.sftp.SftpClientOptions;
import com.sshtools.synergy.ssh.Connection;

public class SshClientCommand extends AbstractSshClientCommand {

	public SshClientCommand() {
		super("ssh", SUBSYSTEM_SHELL, "", "Returns the ssh client shell");
		for (Option option : SftpClientOptions.getOptions()) {
			this.options.addOption(option);
		}
	}
	

	@Override
	public void runCommand(SshClient sshClient, SshClientArguments arguments, VirtualConsole console) {

		console.getSessionChannel().enableRawMode();
		
		try {
			Connection<SshClientContext> connection = sshClient.getConnection();		
			AbstractSessionTask<?> task;
			
			if (CommandUtil.isNotEmpty(arguments.getCommand())) {
				
				String command = arguments.getCommand();
				task = new AbstractCommandTask(connection, command) {
	
					private WindowSizeChangeListener listener;
					
					@Override
					protected void beforeExecuteCommand(SessionChannelNG session) {
						session.allocatePseudoTerminal(console.getTerminal().getType(), 
								console.getTerminal().getWidth(),
								console.getTerminal().getHeight());
					}
					
					@Override
					protected void onOpenSession(SessionChannelNG session) throws IOException {

						listener = new WindowSizeChangeListener() {
							@Override
							public void newSize(int rows, int cols) {
								session.changeTerminalDimensions(cols, rows, 0, 0);
							}
						};
						((VirtualShellNG)console.getSessionChannel()).addWindowSizeChangeListener(listener);
		
	
						con.addTask(new ConnectionAwareTask(con) {
							@Override
							protected void doTask() throws Throwable {
								IOUtils.copy(console.getSessionChannel().getInputStream(), session.getOutputStream());
							}
						});
						IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
					}

					@Override
					protected void onCloseSession(SessionChannelNG session) {
						((VirtualShellNG)console.getSessionChannel()).removeWindowSizeChangeListener(listener);
					}
				};
				
			} else {
				var listener = new WindowSizeChange();
				task = ShellTaskBuilder.create().
						withTermType(console.getTerminal().getType()).
						withColumns(console.getTerminal().getWidth()).
						withRows(console.getTerminal().getHeight()).
						onOpen((t, session) -> {
							listener.session = session;
							((VirtualShellNG)console.getSessionChannel()).addWindowSizeChangeListener(listener);
							connection.addTask(Task.ofRunnable(connection, (c) -> IOUtils.copy(console.getSessionChannel().getInputStream(), session.getOutputStream())));
							IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
						}).
						onClose((t, session) -> ((VirtualShellNG)console.getSessionChannel()).removeWindowSizeChangeListener(listener)).
						build();
			}
	
			connection.addTask(task);
			task.waitForever();

		} finally {
			console.getSessionChannel().disableRawMode();
			console.println();
		}

	}


	@Override
	protected SshClientArguments generateCommandArguments(CommandLine cli, String[] args) throws IOException, PermissionDeniedException {
		return SshClientOptionsEvaluator.evaluate(cli, args, console);
	}

	
	class WindowSizeChange implements WindowSizeChangeListener {
		
		SessionChannelNG session;

		@Override
		public void newSize(int rows, int cols) {
			if(session != null)
				session.changeTerminalDimensions(cols, rows, 0, 0);
		}
		
	}
}
