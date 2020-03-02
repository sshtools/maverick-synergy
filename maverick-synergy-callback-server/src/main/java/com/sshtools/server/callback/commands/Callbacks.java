package com.sshtools.server.callback.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Callbacks extends CallbackCommand {

	public Callbacks() {
		super("callbacks", "Callback", "callbacks", "List the connected callback clients");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		for(SshConnection con : server.getCallbackClients()) {
			console.println(String.format("%-20s %-15s", con.getUsername(), con.getRemoteAddress()));
		}
	}

}
