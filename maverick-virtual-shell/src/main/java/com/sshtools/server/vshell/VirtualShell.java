/* HEADER */
package com.sshtools.server.vshell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.maverick.sshd.ConnectionManager;
import com.maverick.sshd.SessionChannelLG;
import com.maverick.sshd.agent.AgentForwardingChannel;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.terminal.TerminalOutput;

public class VirtualShell extends SessionChannelLG {

	String shellCommand = null;
	
	public VirtualShell(SshConnection con,
			ShellCommandFactory commandFactory, 
			VirtualProcessFactory processFactory,
			String shellCommand) {
		this(con, commandFactory, processFactory);
		this.shellCommand = shellCommand;
	}
	
	public interface WindowSizeChangeListener {
		void newSize(int rows, int cols);
	}

	RootShell shell;
	ShellCommandFactory commandFactory;
	VirtualProcessFactory processFactory;
	TerminalOutput term;
	Map<String, String> env = new HashMap<String, String>();
	List<WindowSizeChangeListener> listeners = new ArrayList<WindowSizeChangeListener>();
	
	public VirtualShell(SshConnection con,
			ShellCommandFactory commandFactory, 
			VirtualProcessFactory processFactory) {
		super(con);
		this.commandFactory = commandFactory;
		this.processFactory = processFactory;
		try {
			this.term = new TerminalOutput(new ShellOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Could not get terminal I/O.");
		}
	}
	
	public VirtualProcessFactory getProcessFactory() {
		return processFactory;
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
			shell.execCommand(getInputStream(), term, cmd, this);
			populateEnvironment();
			return true;
		} catch (Exception e) {
			if(Log.isErrorEnabled())
				Log.error("Failed to execute command " + cmd, e);
		}
		return false;
	}

	protected void changeWindowDimensions(int cols, int rows, int width, int height) {
		this.term.setCols(cols);
		this.term.setRows(rows);
		this.term.setWidth(width);
		this.term.setHeight(height);
		for (WindowSizeChangeListener l : listeners) {
			l.newSize(rows, cols);
		}
	}

	public void onSessionOpen() {
		shell.start();
	}

	protected boolean startShell() {
		
		if(StringUtils.isNotBlank(shellCommand)) {
			return executeCommand(shellCommand);
		}
		
		try {
			shell = commandFactory.createShell(ConnectionManager.getInstance().getConnectionById(getSessionIdentifier()));
			
			// Look for a keybindings for the current terminal
			InputStream keyBindingsStream = null;
			try {
				String type = term.getTerm();
				if(type.startsWith("xterm")) {
					type = "xterm";
				}
				String keyBindingsResource = "/jline/" + type + ".properties";
				keyBindingsStream = getClass().getResource(keyBindingsResource).openStream();
				
			} catch (Exception e) {
			}
			if(keyBindingsStream==null) {
				keyBindingsStream = getClass().getResource("/jline/vt100.properties").openStream();
			}
			shell.startShell(getInputStream(), term, this);
			this.shell.setKeyBindings(keyBindingsStream);
			populateEnvironment();
			return true;
		} catch (Throwable t) {
			Log.warn("Failed to start shell.", t);
		}
		return false;
	}
	
	@Override
	protected boolean requestAgentForwarding(String requestType) {
		
		try {
			if(!con.containsProperty(AgentForwardingChannel.SSH_AGENT_CLIENT)) {
				connection.openChannel(new AgentForwardingChannel(requestType, this));
			} 
			return true;
		} catch (IOException e) {
			return false;
		}
		
	}

	protected boolean allocatePseudoTerminal(String term, int cols, int rows, int width, int height, byte[] modes) {
		try {
			this.term.setTerminal(term);
			this.term.setCols(cols);
			this.term.setRows(rows);
			this.term.setWidth(width);
			this.term.setHeight(height);
			this.term.setModes(modes);

		} catch (IOException ex) {
			Log.warn("Failed to set terminal modes.", ex);
			return false;
		}
		return true;
	}

	protected boolean setEnvironmentVariable(String name, String value) {
		if (shell == null) {
			env.put(name, value);
		} else {
			shell.getProcess().getEnvironment().put(name, value);
		}
		return true;
	}

	class ShellOutputStream extends OutputStream {
		public synchronized void write(byte[] buf, int off, int len) {
			sendData(buf, off, len);
		}

		public void write(int b) {
			write(new byte[] { (byte) b },0,1);
		}
	}

	protected void onChannelOpenFailure() {

	}

	private void populateEnvironment() {
		for (String key : env.keySet()) {
			shell.getProcess().getEnvironment().put(key, env.get(key));
		}
	}

	@Override
	protected void processSignal(String signal) {
		
	}

	@Override
	protected void onLocalEOF() {
		
	}
}
