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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualSessionPolicy;
import com.sshtools.server.vsession.VirtualShellNG;
import com.sshtools.server.vsession.VirtualShellNG.WindowSizeChangeListener;

public class Shell extends ShellCommand {

	private PtyProcess pty;

	public Shell() {
		super("osshell", ShellCommand.SUBSYSTEM_SYSTEM, "osshell", "Run a native shell");
		setDescription("The current operating systems shell");
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

		setScreenSize(console.getTerminal().getWidth(),
				console.getTerminal().getHeight());

		// Listen for window size changes
		VirtualShellNG shell = (VirtualShellNG) console.getSessionChannel();
		WindowSizeChangeListener listener = new WindowSizeChangeListener() {
			public void newSize(int rows, int cols) {
				setScreenSize(cols, rows);
			}
		};
		
		shell.addWindowSizeChangeListener(listener);

		
		console.getSessionChannel().enableRawMode();
		
		console.getSessionChannel().addEventListener(new ChannelEventListener() {

			@Override
			public void onChannelDataIn(Channel channel, ByteBuffer buffer) {
				
				byte[] tmp = new byte[buffer.remaining()];
				buffer.get(tmp);
				Log.info(Utils.bytesToHex(tmp, 32, true, true));
				try {
					out.write(tmp);
					out.flush();
				} catch (IOException e) {
					Log.error("Error writing data to pty", e);
					IOUtils.closeStream(out);
					IOUtils.closeStream(in);
				}
			}
		});

		try {
			IOUtils.copy(in, console.getSessionChannel().getOutputStream());
			out.close();

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

	private void setScreenSize(int width, int height) {
		try {
			pty.setWinSize(new WinSize(width, height));
		} catch (Exception e) {
			Log.warn(String.format("Could not set new terminal size of pty to %d x %d.", width, height));

		}
	}

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
}