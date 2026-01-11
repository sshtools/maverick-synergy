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
import com.sshtools.common.util.Utils;
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
	private VirtualConsole console;
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
	
	protected VirtualConsole getConsole() {
		return console;
	}

	private void runCommand(String cmd, List<String> cmdArgs,
			VirtualConsole console) throws IOException {
		
		this.console = console;
		List<String> args = configureCommand(cmd, cmdArgs, console);
		
		if (cmd == null) {
			cmd = "";
		} else {
			while (cmd.startsWith("/")) {
				cmd = cmd.substring(1);
			}
		}

		pty = startPtyProcess(args);

		
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

		console.getSessionChannel().pauseDataCaching();
		
		ChannelEventListener l = new ChannelEventListener() {

			@Override
			public void onChannelDataIn(Channel channel, ByteBuffer buffer) {
				try {
					writeToCommand(out, buffer);
				} catch (IOException e) {
					Log.error("Error writing data to pty", e);
					IOUtils.closeStream(out);
					IOUtils.closeStream(in);
				}
			}
		};
		console.getSessionChannel().addEventListener(l);

		try {
			runInput(in, console.getSessionChannel().getOutputStream());
			int result = pty.waitFor();
			if (result > 0) {
				throw new IOException("System command exited with error " + result);
			}
		} catch (Exception e) {
			Log.error("Captured error during shell input read", e);
		} finally {
			IOUtils.closeStream(out);
			try {
				console.getSessionChannel().resumeDataCaching();
			}
			finally {
				console.getSessionChannel().removeEventListener(l);
			}
		}
	}
	
	protected PtyProcess startPtyProcess(List<String> args) throws IOException {
		return createPtyProcess(args).start();
	}

	protected PtyProcessBuilder createPtyProcess(List<String> args) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Executing {}", Utils.csv(" ", args));
		}
		Map<String, String> penv = this.env == null ? new HashMap<String, String>(System.getenv()) : new HashMap<String, String>(this.env);
		penv.put("TERM", console.getTerminal().getType());
		configureEnvironment(penv);
		var builder = new PtyProcessBuilder(args.toArray(new String[0]));
		if(directory != null)
			builder.setDirectory(directory.getAbsolutePath());
		builder.setConsole(false);
		builder.setEnvironment(penv);
		return builder;
	}

	protected void configureEnvironment(Map<String, String> penv) {
		
	}

	protected void runInput(InputStream in, OutputStream out) throws IOException {
		IOUtils.copy(in, console.getSessionChannel().getOutputStream());
	}

	protected void writeToCommand(OutputStream out, ByteBuffer buffer) throws IOException {
		byte[] tmp = new byte[buffer.remaining()];
		buffer.get(tmp);
		out.write(tmp);
		out.flush();
	}

	protected List<String> configureCommand(String cmd, List<String> cmdArgs, VirtualConsole console) throws IOException {
		
		List<String> args = new ArrayList<>();
		String shellCommand = ShellUtils.findCommand(getName());
		if(shellCommand == null)
			throw new IOException("Cannot find command " + getName());

		args.add(shellCommand);
		if(cmdArgs!=null) {
			args.addAll(cmdArgs);
		}
		
		return args;
	}


	protected void setScreenSize(int width, int height) {
		try {
			pty.setWinSize(new WinSize(width, height));
		} catch (Exception e) {
			Log.warn(String.format("Could not set new terminal size of pty to %d x %d.", width, height));

		}
	}


}
