package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.tasks.AbstractSessionTask;
import com.sshtools.client.tasks.CommandTask.CommandTaskBuilder;
import com.sshtools.client.tasks.ShellTask.ShellTaskBuilder;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
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
	
		try {
			
			console.getSessionChannel().pauseDataCaching();
			
			Connection<SshClientContext> connection = sshClient.getConnection();		
			AbstractSessionTask<?> task;
			
			var listener = new WindowSizeChange();
			
			if (CommandUtil.isNotEmpty(arguments.getCommand())) {
				String command = arguments.getCommand();
				task = CommandTaskBuilder.create()
						.withConnection(connection)
						.withCommand(command)
						.withTermType(console.getTerminal().getType())
						.withColumns(console.getTerminal().getWidth())
						.withRows(console.getTerminal().getHeight())
						.onBeforeExecute((t, session)->{
							listener.session = session;
							((VirtualShellNG)console.getSessionChannel()).addWindowSizeChangeListener(listener);
						})
						.onTask((t, session)->{
							connection.addTask(Task.ofRunnable(connection, (c) -> IOUtils.copy(console.getSessionChannel().getInputStream(), session.getOutputStream())));
							IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
							
						})
						.onClose((t, session) -> ((VirtualShellNG)console.getSessionChannel()).removeWindowSizeChangeListener(listener))
						.build();
				
			} else {
				
				task = ShellTaskBuilder.create().
						withConnection(connection).
						withTermType(console.getTerminal().getType()).
						withColumns(console.getTerminal().getWidth()).
						withRows(console.getTerminal().getHeight()).
						onBeforeTask((t, session) -> {
							listener.session = session;
							((VirtualShellNG)console.getSessionChannel()).addWindowSizeChangeListener(listener);
							
							ChannelEventListener l = new ChannelEventListener() {

								@Override
								public void onChannelDataIn(Channel channel, ByteBuffer buffer) {

									byte[] tmp = new byte[buffer.remaining()];
									buffer.get(tmp);

									try {
										session.getOutputStream().write(tmp);
										session.getOutputStream().flush();
									} catch (IOException e) {
										Log.error("Error writing data from console", e);
									}
								}
							};
							console.getSessionChannel().addEventListener(l);
							
							session.addEventListener(new ChannelEventListener() {
								@Override
								public void onChannelClose(Channel channel) {
									if(Log.isDebugEnabled()) {
										Log.debug("Detected close of child command so removing channel data listeners");
									}
 									console.getSessionChannel().removeEventListener(l);
									session.removeEventListener(this);
								}
							});
						})
						.onTask((t, session)-> {
							IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
						}).
						onClose((t, session) -> ((VirtualShellNG)console.getSessionChannel()).removeWindowSizeChangeListener(listener)).
						build();
			}
	
			connection.addTask(task);
			task.waitForever();

		} finally {

			console.getSessionChannel().resumeDataCaching();
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
