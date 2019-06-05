package com.sshtools.server.vshell.commands.admin;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Cell;
import com.sshtools.server.vshell.terminal.Row;
import com.sshtools.server.vshell.terminal.Table;

public class Connections<T extends AbstractFile> extends ShellCommand {

	public Connections() {
		super("con", ShellCommand.SUBSYSTEM_SSHD);
		setDescription("Show active connections");
	}

	public void run(CommandLine args, VirtualProcess process)
			throws IOException, PermissionDeniedException {

		Table table = new Table(process.getTerminal());
		Row header = new Row(new Cell<String>("UUID"));
		table.setHeader(header);
		for(SshConnection c : process.getConnection().getConnectionManager().getAllConnections()) {
			table.add(new Row(new Cell<String>(c.getUUID())));
		}
		
		table.render(process.getConsole());
	}

}
