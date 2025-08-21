package com.sshtools.server.vsession.commands.os;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.SessionChannelNG;
import com.sshtools.synergy.ssh.TerminalModes;

public class NativeSessionChannel extends SessionChannelNG {

	private PtyProcess pty;
	private Map<String, String> env;
	private File directory;

	String term = "dumb";
	int cols = 80;
	int rows = 25;

	final String commandProgram = "$SYSTEMROOT\\System32\\cmd.exe";
	final String powershellProgram = "$SYSTEMROOT\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
	final String powershellEncoding = "ISO-8859-1";
	
	public NativeSessionChannel(SshConnection con) {
		super(con);
	}

	@Override
	protected void changeWindowDimensions(int cols, int rows, int width, int height) {
		setScreenSize(cols, rows);
	}

	@Override
	protected void processSignal(String signal) {

	}

	@Override
	public boolean setEnvironmentVariable(String name, String value) {
		return false;
	}

	@Override
	protected boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height,
			TerminalModes modes) {
		this.term = term;
		this.cols = cols;
		this.rows = rows;
		return true;
	}

	private void setScreenSize(int width, int height) {
		try {
			pty.setWinSize(new WinSize(width, height));
		} catch (Exception e) {
			Log.warn(String.format("Could not set new terminal size of pty to %d x %d.", width, height));

		}
	}

	@Override
	protected boolean startShell() {

		try {
			List<String> args = configureCommand("", Collections.emptyList());

			Map<String, String> penv = this.env == null ? new HashMap<String, String>(System.getenv())
					: new HashMap<String, String>(this.env);
			penv.put("TERM", term);

			var builder = new PtyProcessBuilder(args.toArray(new String[0]));
			if (directory != null)
				builder.setDirectory(directory.getAbsolutePath());
			builder.setConsole(false);
			builder.setEnvironment(penv);
			pty = builder.start();

			final InputStream in = pty.getInputStream();
			final OutputStream out = pty.getOutputStream();

			setScreenSize(cols, rows);

			pauseDataCaching();

			ChannelEventListener l = new ChannelEventListener() {

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
			};

			addEventListener(l);

			getContext().getExecutorService().submit(() -> {
				try {
					IOUtils.copy(in, getOutputStream());
					out.close();

					int result = pty.waitFor();
					if (result > 0) {
						throw new IOException("System command exited with error " + result);
					}
				} catch (Exception e) {
				} finally {
					try {
						resumeDataCaching();
					} finally {
						removeEventListener(l);
					}
				}
			});

			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected void onLocalEOF() {

	}

	private String generateWindowsPath(String path) {
		return path.replace("$SYSTEMROOT", StringUtils.defaultIfBlank(System.getenv("SystemRoot"), "C:\\Windows"));
	}

	protected List<String> configureCommand(String cmd, List<String> cmdArgs)
			throws IOException {

		List<String> args = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			if (StringUtils.isBlank(cmd)) {
				String path = generateWindowsPath(powershellProgram);
				if (Files.exists(Path.of(path))) {
					args.add(path);
				} else {
					args.add(generateWindowsPath(commandProgram));
				}
			} else {
				args.add(cmd);
			}
		} else {

			if (SystemUtils.IS_OS_MAC_OSX) {
				if (StringUtils.isBlank(cmd)) {
					cmd = findCommand("zsh", "/bin/zsh", "bash", "/usr/bin/bash", "/bin/bash", "sh",
							"/usr/bin/sh", "/bin/sh");
					if (cmd == null)
						throw new IOException("Cannot find OSX shell.");
				}
			} else {
				if (StringUtils.isBlank(cmd)) {
					cmd = findCommand("bash", "/usr/bin/bash", "/bin/bash", "sh", "/usr/bin/sh", "/bin/sh");
					if (cmd == null)
						throw new IOException("Cannot find shell.");
				}
			}

			args.add(cmd);
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
