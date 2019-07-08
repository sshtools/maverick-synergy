package com.sshtools.server.vsession;

import java.io.IOException;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;

public class VirtualConsole {

	Environment env;
	Terminal terminal;
	LineReader reader;
	SshConnection con;
	Channel channel;
	Msh shell;
	
	VirtualConsole(Channel channel, Environment env, Terminal terminal, LineReader reader, Msh shell) {
		this.channel = channel;
		this.con = channel.getConnection();
		this.env = env;
		this.terminal = terminal;
		this.reader = reader;
		this.shell = shell;
	}
	
	public SshConnection getConnection() {
		return con;
	}
	
	public Environment getEnvironment() {
		return env;
	}
	
	public Terminal getTerminal() {
		return terminal;
	}
	
	public LineReader getLineReader() {
		return reader;
	}
	
	public void clear() {
		terminal.puts(Capability.clear_screen);
        terminal.flush();
	}
	
	public void print(String str) {
		terminal.writer().print(str);
		terminal.writer().flush();
	}
	
	public void println(String str) {
		terminal.writer().println(str);
		terminal.writer().flush();
	}
	
	public void println() {
		terminal.writer().println();
		terminal.writer().flush();
	}
	
	public String readLine(String prompt) {
		return reader.readLine(prompt);
	}

	public Context getContext() {
		return con.getContext();
	}

	public String readLine() {
		return reader.readLine();
	}

	public AbstractFile getCurrentDirectory() {
		try {
			return getContext().getFileFactory().getFile((String)env.getOrDefault("HOME", ""), con);
		} catch (PermissionDeniedException | IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public Channel getSessionChannel() {
		return channel;
	}

	public History getHistory() {
		return reader.getHistory();
	}

	public void destroy() {

	}

	public void print(Throwable e) {
		e.printStackTrace(terminal.writer());
		terminal.writer().flush();
		
	}

	public Msh getShell() {
		return shell;
	}
	
}
