package com.sshtools.server.vshell.commands;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.login.LoginContext;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Cell;
import com.sshtools.server.vshell.terminal.Row;
import com.sshtools.server.vshell.terminal.Table;

public class Who<T extends AbstractFile> extends ShellCommand {
	public static final String LOGIN_CONTEXT = "loginContext";

	public Who() {
		super("who", SUBSYSTEM_SHELL, "[<username>|am i]");
		setDescription("List all logged on users, show your own details or show login details");
		setBuiltIn(false);
	}

	public void run(CommandLine cli, VirtualProcess process) throws IOException {

		String[] args = cli.getArgs();
		if (args.length == 1) {
			Table table = createTable(process);
			for (SshConnection connection : process.getConnection().getConnectionManager().getAllConnections()) {
				addRow(table, connection);
			}

			table.render(process.getConsole());
		} else {
			if (args.length == 3 && args[1].equalsIgnoreCase("am")
					&& args[2].equalsIgnoreCase("i")) {
				Table table = createTable(process);
				addRow(table, process.getConnection());
				table.render(process.getConsole());
			} else {
				for (SshConnection connection : process.getConnection().getConnectionManager().getAllConnections()) {
					for (int i = 1; i < args.length; i++) {
						if (connection.getUsername().equals(args[i])) {
							LoginContext ctx = (LoginContext) process
									.getConnection().getProperty(LOGIN_CONTEXT);
							process.getConsole().printStringNewline(
									"Principals :-");
							for (Principal p : ctx.getSubject().getPrincipals()) {
								process.getConsole().printStringNewline(
										"    " + p.getName());
							}
							process.getConsole().printStringNewline(
									"Public Credentials :-");
							for (Object p : ctx.getSubject()
									.getPublicCredentials()) {
								process.getConsole().printStringNewline(
										"    " + p.toString());
							}
							break;
						}
					}
				}
			}
		}
	}

	private void addRow(Table table, SshConnection connection) {
		table.add(new Row(new Cell<String>(connection.getUsername()),
				new Cell<String>(connection.getRemoteAddress().toString()),
				new Cell<String>(connection.getSessionId())));
	}

	private Table createTable(VirtualProcess process) {
		Table table = new Table(process.getTerminal());
		Row header = new Row(new Cell<String>("Username"), new Cell<String>(
				"Address"), new Cell<String>("UUID"));
		table.setHeader(header);
		return table;
	}
}
