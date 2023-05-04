package com.sshtools.server.callback.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.callback.Callback;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Callbacks extends CallbackCommand {

	public Callbacks() {
		super("callbacks", "Callback", "callbacks", "List the connected callback clients");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		for(Callback con : service.getCallbacks()) {
			console.println(String.format("%-25s %-15s %s/%s", con.getUuid(), con.getRemoteAddress(), con.getUsername(), con.getMemo()));
		}
	}

}
