package com.sshtools.server.vshell.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.Terminal;

import com.sshtools.server.vshell.CommandFactory;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Console extends ConsoleReader {

	CommandFactory<ShellCommand> commandFactory;

	public Console(InputStream in, OutputStream out, InputStream bindings, Terminal terminal, CommandFactory<ShellCommand> commandFactory) throws IOException {
		super(in, out, bindings, terminal);
		this.commandFactory = commandFactory;
	}


	public void init(VirtualProcess process) {
		setEOL(EOL_CRLF);
		setUseHistory(true);
		Completor[] completors = { new CommandCompletor<ShellCommand>(process, commandFactory, process.getConnection()), new CommandArgsCompletor(process) };
		addCompletor(new ArgumentCompletor(completors));
	}
}
