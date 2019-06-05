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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
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
