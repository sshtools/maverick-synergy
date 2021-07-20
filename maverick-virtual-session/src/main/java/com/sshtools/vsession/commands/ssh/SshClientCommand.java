
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
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.VirtualConsole;
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
	protected SshClientArguments generateCommandArguments(CommandLine cli, String[] args) throws IOException, PermissionDeniedException {
		return SshClientOptionsEvaluator.evaluate(cli, args, console);
	}

}
