package com.sshtools.server.vshell;

import java.io.IOException;
import java.io.Reader;

import com.sshtools.server.vshell.terminal.Console;

public class ConsoleStreamReader extends Reader {

	private Console console;
	private String buffer;

	public ConsoleStreamReader(Console console) {
		this.console = console;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		String line = buffer;
		if (line == null || line.length() == 0) {
			line = console.readLine();
		}
		if (line == null) {
			return -1;
		}
		if (line.length() > len) {
			buffer = line.substring(len);
			System.arraycopy(line.toCharArray(), 0, cbuf, off, len);
			return len;
		} else {
			buffer = null;
			System.arraycopy(line.toCharArray(), 0, cbuf, off, line.length());
			return line.length();
		}
	}

	@Override
	public void close() throws IOException {

	}

}