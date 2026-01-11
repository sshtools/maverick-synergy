package com.sshtools.server.vsession.commands.os;

/*-
 * #%L
 * Virtual Sessions
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import com.sshtools.common.util.IOUtils;

public class ShellUtils {

	public final static String execAndCaptureWithException(String... args) throws IOException {
		try {
			ProcessBuilder builder = new ProcessBuilder(args);
			builder.redirectErrorStream();
			Process process = builder.start();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(process.getInputStream(), out);
			int ret = process.waitFor();
			if (ret == 0) {
				return new String(out.toByteArray());
			}
			throw new IOException(new String(out.toByteArray()));
		} catch(InterruptedException e) {
			throw new InterruptedIOException("The process was interrupted!");
		}
	}
	
	public final static String execAndCapture(String... args) throws IOException {
		try {
			ProcessBuilder builder = new ProcessBuilder(args);
			builder.redirectErrorStream();
			Process process = builder.start();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(process.getInputStream(), out);
			int ret = process.waitFor();
			if (ret == 0) {
				return new String(out.toByteArray());
			}
		} catch(Exception e) {
			throw new InterruptedIOException("The process was interrupted!");
		}
		return null;
	}
	
	public static String findInstalledCommand(String command) throws IOException {
		File cwd = new File(".").getCanonicalFile();
		File cwdCommand = new File(cwd, CommandLocator.getCommandPath(command).toString());
		if(cwdCommand.exists()) {
			return cwdCommand.getAbsolutePath();
		}
		throw new IllegalStateException(String.format("Cannot find %s installed in %s", command, cwdCommand.getAbsolutePath()));
	}
	
	public static String findCommand(String command, String... places) throws IOException {
		File cwd = new File(".").getCanonicalFile();
		File cwdCommand = new File(cwd, command);
		if(cwdCommand.exists()) {
			return cwdCommand.getAbsolutePath();
		}
		String stdbuf = execAndCapture("which", command);
		if (stdbuf == null) {
			for (String place : places) {
				File f = new File(place);
				if (f.exists()) {
					stdbuf = f.getAbsolutePath();
					break;
				}
			}
		}
		if (stdbuf != null) {
			while (stdbuf.endsWith("\n")) {
				stdbuf = stdbuf.substring(0, stdbuf.length() - 1);
			}
		}
		return stdbuf;
	}

}
