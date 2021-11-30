/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server.vsession.commands.os;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualSessionPolicy;
import com.sshtools.server.vsession.VirtualShellNG;
import com.sshtools.server.vsession.VirtualShellNG.WindowSizeChangeListener;

public class Shell extends ShellCommand {

	private int width;
	private int height;
	private PtyProcess pty;

	public Shell() {
		super("osshell", ShellCommand.SUBSYSTEM_SYSTEM, "osshell", "Run a native shell");
		setDescription("An operating system shell");
		setBuiltIn(false);
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		runCommand(null, null, null, null, console);
	}

	private void runCommand(String cmd, List<String> cmdArgs, File directory, Map<String, String> env,
			VirtualConsole console) throws IOException {
		List<String> args = new ArrayList<String>();
		if (cmd == null) {
			cmd = "";
		} else {
			while (cmd.startsWith("/")) {
				cmd = cmd.substring(1);
			}
		}

		String shellCommand = console.getContext().getPolicy(VirtualSessionPolicy.class).getShellCommand();
		if (SystemUtils.IS_OS_WINDOWS) {
			if(StringUtils.isBlank(shellCommand)) {
				args.add("C:\\Windows\\System32\\cmd.exe");
//				args.add("/c");
//				args.add("start");
//				if (!cmd.equals("")) {
//					args.add(cmd);
//				} else {
//					args.add("cmd.exe");
//				}
			} else {
				args.add(shellCommand);
				args.addAll(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellArguments());
			}
		}
		else { 
			// The shell, should be in /bin but just in case
			if(StringUtils.isBlank(shellCommand)) {
				shellCommand = findCommand("bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh");
				if(shellCommand == null)
					throw new IOException("Cannot find shell.");
			}
		
			args.add(shellCommand);
			args.addAll(console.getContext().getPolicy(VirtualSessionPolicy.class).getShellArguments());
		}

		env = env == null ? new HashMap<String, String>() : new HashMap<String, String>(env);
		env.put("TERM", console.getTerminal().getType());

		pty = PtyProcess.exec(args.toArray(new String[0]), env, directory == null ? null : directory.getAbsolutePath(), false);

		final InputStream in = pty.getInputStream();
		final OutputStream out = pty.getOutputStream();

		width = console.getTerminal().getWidth();
		height = console.getTerminal().getHeight();

		setScreenSize();

		// Listen for window size changes
		VirtualShellNG shell = (VirtualShellNG) console.getSessionChannel();
		WindowSizeChangeListener listener = new WindowSizeChangeListener() {
			public void newSize(int rows, int cols) {
				width = cols;
				height = rows;
				setScreenSize();
			}
		};
		shell.addWindowSizeChangeListener(listener);

		
		console.getSessionChannel().enableRawMode();
		
		console.getConnection().addTask(new ConnectionAwareTask(console.getConnection()) {
			@Override
			protected void doTask() throws Throwable {
				IOUtils.copy(console.getSessionChannel().getInputStream(), out);
			}
		});
		IOUtils.copy(in, console.getSessionChannel().getOutputStream());
//		new IOStreamConnector(process.getTerminal().input(), out);
//		int read = -1;
//		byte[] buf = new byte[IOStreamConnector.DEFAULT_BUFFER_SIZE];
//		try {
//			while ((read = in.read(buf)) > -1) {
//				process..write(buf, 0, read);
//				termIo.getAttachedOutputStream().flush();
//			}
//		} catch (IOException ioe) {
//		}
		out.close();
		try {
			int result = pty.waitFor();
			if (result > 0) {
				throw new IOException("System command exited with error " + result);
			}
		} catch (Exception e) {
		} finally {
			console.getSessionChannel().disableRawMode();
		}
	}

	private String findCommand(String command, String... places) {
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

//	private String escapeArg(String arg) {
//		char[] ch = arg.toCharArray();
//		StringBuffer buf = new StringBuffer();
//		for (int i = 0; i < ch.length; i++) {
//			char c = ch[i];
//			if (c == '\\') {
//				buf.append('\\');
//			} else if (c == '"') {
//				buf.append('\\');
//			}
//			buf.append(c);
//		}
//		return buf.toString();
//	}

	private void setScreenSize() {
		try {
			pty.setWinSize(new WinSize(width, height));
		} catch (Exception e) {
			Log.warn(String.format("Could not set new terminal size of pty to %d x %d.", width, height));

		}
	}

//	private void addShCommand(List<String> args, String cmd, List<String> cmdArgs) {
//		cmd = appendArguments(cmd, cmdArgs);
//		if (!cmd.equals("")) {
//			args.add("-c");
//			args.add(cmd);
//		}
//	}
//
//	private void addArgs(List<String> args, List<String> cmdArgs) {
//		args.addAll(cmdArgs);
//	}
//
//	private String appendArguments(String cmd, List<String> cmdArgs) {
//		if (cmdArgs != null) {
//			for (String arg : cmdArgs) {
//				if (cmd.length() > 0) {
//					cmd += " ";
//				}
//				cmd += "\"" + escapeArg(arg) + "\"";
//			}
//		}
//		return cmd;
//	}

	private final static String execAndCapture(String... args) {
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
			throw new IOException("Got non-zero return status.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

//	private static final class SinkOutputStream extends OutputStream {
//		@Override
//		public void write(int b) throws IOException {
//		}
//	}
}