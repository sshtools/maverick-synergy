package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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
	protected VirtualConsole console;
	protected ShellCommandFactory commandFactory;

	List<WindowSizeChangeListener> listeners = new ArrayList<WindowSizeChangeListener>();
	private Terminal terminal;
	
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

	public void onSessionOpen() {
	
		shell.start();
	}

	protected boolean startShell() {
		
		if(Utils.isNotBlank(shellCommand)) {
			return executeCommand(shellCommand);
		}
		
		try {
			shell = createShell(con);			
			shell.startShell(null, console = createConsole());
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
		
		Attributes attrs = new Attributes();
		attrs.setInputFlag(InputFlag.ICRNL, true);
		terminal = TerminalBuilder.builder().
					system(false).
					streams(getInputStream(), getOutputStream()).
					type(env.getOrDefault("TERM", "ansi").toString()).
					size(new Size(env.getOrDefault("COLS", 80), env.getOrDefault("ROWS", 80))).
					encoding(Charset.forName("UTF-8")).
					attributes(attrs).build();
		
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
	public void pauseDataCaching() {
		console.getTerminal().pause();
		super.pauseDataCaching();
	}

	@Override
	public void resumeDataCaching() {
		super.resumeDataCaching();
		console.getTerminal().resume();
		
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
						| PermissionDeniedException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
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
