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

		console.println(String.format("---- Opening shell on client %s", clientName));
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
		
		console.println();
		console.println(String.format("---- Exited shell on client %s", clientName));
	}

}
