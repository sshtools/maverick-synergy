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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell.commands.script;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ConsoleOutputStream;
import com.sshtools.server.vshell.ConsoleStreamReader;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

import bsh.EvalError;
import bsh.Interpreter;

public class Bsh extends ShellCommand {

	private final static Map<String, Object> beans = new HashMap<String, Object>();
	
	public Bsh() {
		super("bsh", SUBSYSTEM_SHELL, "[<script>]");
		setDescription("Bean shell");
		setBuiltIn(false);
	}
	
	public Map<String, Object> getBeans() {
		return beans;
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException {
		Console reader = process.getConsole();
		String oprompt = reader.getDefaultPrompt();
		ConsoleStreamReader stdIn = new ConsoleStreamReader(reader);
		PrintStream stdOut = new PrintStream(new ConsoleOutputStream(reader));
		PrintStream stdErr = new PrintStream(new ConsoleOutputStream(reader));
		Interpreter interpreter = new Interpreter(stdIn, stdOut, stdErr, true);
		interpreter.setExitOnEOF(false);
		reader.setDefaultPrompt("");
		try {
			interpreter.set("stdout", stdOut);
			interpreter.set("stderr", stdErr);
			interpreter.set("stdin", stdIn);
			for(String key : beans.keySet()) {
				interpreter.set(key, beans.get(key));
			}
			for (String key : process.getEnvironment().keySet()) {
				interpreter.set(key, process.getEnvironment().get(key));
			}
		} catch (EvalError e) {
			throw new IOException("Could not configure environment.");
		}
		try {
			interpreter.run();
		} finally {
			reader.setDefaultPrompt(oprompt);
			process.getConsole().printStringNewline("");
		}
	}

}
