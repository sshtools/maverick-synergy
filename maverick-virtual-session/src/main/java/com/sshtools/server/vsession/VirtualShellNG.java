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

package com.sshtools.server.vsession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.spi.JnaSupport;
import org.jline.terminal.spi.Pty;

import com.sshtools.common.files.nio.AbstractFileURI;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.ClassLoaderPolicy;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.Utils;
import com.sshtools.server.AgentForwardingChannel;
import com.sshtools.server.SessionChannelNG;

public class VirtualShellNG extends SessionChannelNG {

	String shellCommand = null;
	Environment env = new Environment();
	Set<String> protectedEnvironmentVars = new HashSet<>();
	
	public VirtualShellNG(SshConnection con,
			ShellCommandFactory commandFactory, 
			String shellCommand) {
		this(con, commandFactory);
		this.shellCommand = shellCommand;
	}
	
	public interface WindowSizeChangeListener {
		void newSize(int rows, int cols);
	}

	RootShell shell;
	VirtualConsole console;
	ShellCommandFactory commandFactory;

	boolean rawMode = false;
	
	List<WindowSizeChangeListener> listeners = new ArrayList<WindowSizeChangeListener>();
	
	public VirtualShellNG(SshConnection con,
			ShellCommandFactory commandFactory) {
		super(con);
		this.commandFactory = commandFactory;
	}

	public void addWindowSizeChangeListener(WindowSizeChangeListener listener) {
		listeners.add(listener);
	}

	public void removeWindowSizeChangeListener(WindowSizeChangeListener listener) {
		listeners.remove(listener);
	}
	
	public void addProtectedEnvironmentVar(String name) {
		protectedEnvironmentVars.add(name.toUpperCase());
	}

	protected boolean executeCommand(String cmd) {
		
		try {
			shell = commandFactory.createShell(con);
			shell.execCommand(getInputStream(), console = createConsole(), cmd);
			return true;
		} catch (Exception e) {
			if(Log.isErrorEnabled())
				Log.error("Failed to execute command " + cmd, e);
		} 
		return false;
	}

	protected void changeWindowDimensions(int cols, int rows, int width, int height) {
		
		console.getTerminal().setSize(new Size(cols, rows));

		for (WindowSizeChangeListener l : listeners) {
			l.newSize(rows, cols);
		}
	}

	@Override
	protected void onSessionData(ByteBuffer data) {

		byte[] tmp = new byte[data.remaining()];
		data.get(tmp);
		try {
			((PosixChannelPtyTerminal)console.getTerminal()).in(tmp, 0, tmp.length);
			evaluateWindowSpace();
		} catch (IOException e) {
			Log.error("Failed to send input to terminal.", e);
			close();
		}
		
	}


	public void onSessionOpen() {
		shell.start();
	}

	protected boolean startShell() {
		
		if(Utils.isNotBlank(shellCommand)) {
			return executeCommand(shellCommand);
		}
		
		try {
			shell = createShell(con);			
			shell.startShell(getInputStream(), console = createConsole());
			return true;
		} catch (Throwable t) {
			Log.warn("Failed to start shell.", t);
		} 
		return false;
	}

	protected RootShell createShell(SshConnection con) throws PermissionDeniedException, IOException {
		return commandFactory.createShell(con);
	}
	
	private VirtualConsole createConsole() throws IOException, PermissionDeniedException {
		
        Pty pty = load(JnaSupport.class).open(null, null);

        Terminal terminal = new PosixChannelPtyTerminal("Maverick Terminal", 
				env.getOrDefault("TERM", "ansi").toString(), 
				pty,
				(int) env.getOrDefault("COLS", 80),
				(int) env.getOrDefault("ROWS", 25),
				this,
				Charset.forName("UTF-8"));
		
        Map<String,Object> env = new HashMap<>();
		env.put("connection", getConnection());
		FileSystem fs = FileSystems.newFileSystem(
				AbstractFileURI.create(getConnection(), ""), 
				env,
				getContext().getPolicy(ClassLoaderPolicy.class).getClassLoader());
		
        final LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new VirtualShellCompletor())
                .variable(LineReader.HISTORY_SIZE, 1000)
                .variable(LineReader.HISTORY_FILE, fs.getPath(".history"));

		return new VirtualConsole(this, this.env, terminal, lineReaderBuilder.build(), shell);
	}

    private <S> S load(Class<S> clazz) {
        return ServiceLoader.load(clazz, clazz.getClassLoader()).iterator().next();
    }


	@Override
	protected boolean requestAgentForwarding(String requestType) {
		
		try {
			if(!getConnection().containsProperty(AgentForwardingChannel.SSH_AGENT_CLIENT)) {
				connection.openChannel(new AgentForwardingChannel(requestType, this));
			} 
			return true;
		} catch (IOException e) {
			return false;
		}
		
	}

	protected boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height, byte[] modes) {
			
			env.put("TERM", term);
			env.put("COLS", cols);
			env.put("ROWS", rows);
			env.put("PTYMODES", modes);
	
			return true;
	}

	public boolean setEnvironmentVariable(String name, String value) {
		if(protectedEnvironmentVars.contains(name.toUpperCase())) {
			return false;
		}
		env.put(name, value);
		return true;
	}

	protected void onChannelOpenFailure() {

	}


	@Override
	protected void processSignal(String signal) {
		
	}

	@Override
	protected void onLocalEOF() {
		
	}

	@Override
	protected void onChannelClosed() {
	}
	
	class VirtualShellCompletor implements Completer, MshListener {

		Command currentCommand = null;
		AtomicBoolean inCommand = new AtomicBoolean();
		VirtualShellCompletor() {
			shell.addListener(this);
		}
		
		@Override
		public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
			
			if(!inCommand.get()) {
				processShellCompletion(reader, line, candidates);
			} else {
				processInCommandCompletion(reader, line, candidates);
			}
		}
		
		private void processInCommandCompletion(LineReader reader, ParsedLine line, List<Candidate> candidates) {
			@SuppressWarnings("unchecked")
			List<Candidate> tmp = (List<Candidate>) console.getEnvironment().get("_COMPLETIONS");
			if(Objects.nonNull(tmp)) {
				candidates.addAll(tmp);
			}
		}

		private void processShellCompletion(LineReader reader, ParsedLine line, List<Candidate> candidates) {
			
			switch(line.wordIndex()) {
			case 0:
				/**
				 * This is a possible command
				 */
				for(String cmd : commandFactory.getSupportedCommands()) {
					candidates.add(new Candidate(cmd));
				}
				break;
			default:
				/**
				 * Defer to command about to be executed
				 */
				try {
					ShellCommand cmd = commandFactory.createCommand(line.words().get(0), con);
					cmd.complete(reader, line, candidates);
				} catch (IllegalAccessException | InstantiationException
						| UnsupportedCommandException | IOException
						| PermissionDeniedException e) {
				}
			}
		}

		@Override
		public void commandStarted(Command cmd, String[] args, VirtualConsole console) {
			inCommand.set(true);
			this.currentCommand = cmd;
		}
		
		@Override
		public void commandFinished(Command cmd, String[] args, VirtualConsole console) {
			inCommand.set(false);
			this.currentCommand = null;
		}
		
	}
}
