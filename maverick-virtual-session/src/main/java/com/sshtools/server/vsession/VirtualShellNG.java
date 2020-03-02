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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
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
	
	private VirtualConsole createConsole() throws IOException {
		
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

	protected boolean setEnvironmentVariable(String name, String value) {
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
}
