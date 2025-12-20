package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

/**
 * Utility to prompt for input from the console (standard input). While Java has
 * {@link Console}, it does not fall back to basic standard input if it is not
 * available. This utility does that, allowing Maverick Synergy examples (and
 * users if they want) to securely prompt for passwords and passphrases where
 * possible.
 */
public class ConsoleInput {
	/**
	 * Convenience method to prompt for a password from the console. The password
	 * will be masked if possible.   
	 * 
	 * @param message prompt text
	 * @return password or <code>null</code>
	 */
	public static char[] promptPassword(String message) {
		return ConsoleInput.get().readPassword("%s: ", message);
	}
	
	/**
	 * Convenience method to prompt for input from the console.   
	 * 
	 * @param reader reader
	 * @param message prompt text
	 * @return text or <code>null</code>
	 */
	public static String promptText(String message) {
		return ConsoleInput.get().readLine("%s: ", message);
	}
	
	/**
	 * Convenience method to prompt for input from the console.   
	 * 
	 * @param reader reader
	 * @param message prompt text
	 * @return text or <code>null</code>
	 */
	public static String promptText(String message, String defaultValue) throws IOException {
		var line = ConsoleInput.get().readLine("%s [%s]: ", message, defaultValue);
		if(Utils.isBlank(line)) {
			return defaultValue;
		}
		return line;
	}
	

	/**
	 * For lazy creation
	 */
	private static class Defaults {
		private final static ConsoleInput INSTANCE = new ConsoleInput();
	}

	public static ConsoleInput get() {
		return Defaults.INSTANCE;
	}

	private final Console systemConsole;
	private final BufferedReader systemReader;

	private ConsoleInput() {
		systemConsole = System.console();
		systemReader = systemConsole == null ? new BufferedReader(new InputStreamReader(System.in)) : null;
	}
	
	/**
	 * Remove when {@link Utils#prompt(BufferedReader, String)} and friends are removed.
	 * 
	 * @param systemConsole system console
	 * @param systemReader system reader
	 */
	@Deprecated
	ConsoleInput(Console systemConsole, BufferedReader systemReader) {
		this.systemConsole = systemConsole;
		this.systemReader = systemReader;
	}
	
	/**
	 * Get if the full system {@link Console} is available (and passwords will be masked).
	 * 
	 * @return console available
	 */
	public boolean isConsole() {
		return systemConsole != null;
	}

	/**
	 * Read a password from the console with no additional prompt text. The input will
	 * be masked if the {@link Console} is available, otherwise input will be fully visible.
	 * 
	 * @return password or <code>null</code> if neither the console or standard input is available.
	 */
	public char[] readPassword() {
		if (systemConsole == null) {
			return readerPassword();
		} else {
			return systemConsole.readPassword();
		}
	}

	/**
	 * Read a password from the console after printing some formatted prompt text. 
	 * The text can be formatted using the same rules as {@link String#format}. The input will
	 * be masked if the {@link Console} is available, otherwise input will be fully visible.
	 * 
	 * @param fmt prompt text formatter pattern
	 * @return password or <code>null</code> if neither the console or standard input is available.
	 */
	public char[] readPassword(String fmt, Object... args) {
		if (systemConsole == null) {
			System.out.format(fmt, args);
			return readerPassword();
		} else {
			return systemConsole.readPassword(fmt, args);
		}
	}
	
	/**
	 * Read input from the console with no additional prompt text.
	 * 
	 * @return input or <code>null</code> if neither the console or standard input is available.
	 */
	public String readLine() {
		if (systemConsole == null) {
			return readerLine();
		} else {
			return systemConsole.readLine();
		}
	}

	/**
	 * Read input from the console after printing some formatted prompt text. 
	 * The text can be formatted using the same rules as {@link String#format}. 
	 * 
	 * @param fmt prompt text formatter pattern
	 * @return input or <code>null</code> if neither the console or standard input is available.
	 */
	public String readLine(String fmt, Object... args) {
		if (systemConsole == null) {
			System.out.format(fmt, args);
			return readerLine();
		} else {
			return systemConsole.readLine(fmt, args);
		}
	}

	private char[] readerPassword() {
		var line = readerLine();
		return line == null ? null : line.toCharArray();
	}

	private String readerLine() {
		try {
			return systemReader.readLine();
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
}
