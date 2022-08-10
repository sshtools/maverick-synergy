/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.server.vsession.commands.os;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.server.vsession.VirtualShellNG;
import com.sshtools.server.vsession.VirtualShellNG.WindowSizeChangeListener;

public class AbstractOSCommand extends ShellCommand {

	public AbstractOSCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}

	private PtyProcess pty;
	private Map<String, String> env;
	private File directory;
	
	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		runCommand(null, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)), console);
	}
	
	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	private void runCommand(String cmd, List<String> cmdArgs,
			VirtualConsole console) throws IOException {
		
		List<String> args = configureCommand(cmd, cmdArgs, console);
		
		if (cmd == null) {
			cmd = "";
		} else {
			while (cmd.startsWith("/")) {
				cmd = cmd.substring(1);
			}
		}

		Map<String, String> penv = this.env == null ? new HashMap<String, String>() : new HashMap<String, String>(this.env);
		penv.put("TERM", console.getTerminal().getType());

		var builder = new PtyProcessBuilder(args.toArray(new String[0]));
		if(directory != null)
			builder.setDirectory(directory.getAbsolutePath());
		builder.setConsole(false);
		builder.setEnvironment(env);
		pty = builder.start();

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

	protected List<String> configureCommand(String cmd, List<String> cmdArgs, VirtualConsole console) throws IOException {
		
		List<String> args = new ArrayList<>();
		String shellCommand = findCommand(getName());
		if(shellCommand == null)
			throw new IOException("Cannot find command " + getName());

		args.add(shellCommand);
		if(cmdArgs!=null) {
			args.addAll(cmdArgs);
		}
		
		return args;
	}

	protected String findCommand(String command, String... places) {
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