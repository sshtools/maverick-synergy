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