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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SshConnection;

public class VirtualConsole {

	Environment env;
	Terminal terminal;
	LineReader reader;
	SshConnection con;
	SessionChannelServer channel;
	Msh shell;
	AbstractFile cwd;
	AbstractFileFactory<?> fileFactory;
	
	static ThreadLocal<VirtualConsole> threadConsoles = new ThreadLocal<>();
	
	public VirtualConsole(SessionChannelServer channel, Environment env, Terminal terminal, LineReader reader, Msh shell) throws IOException, PermissionDeniedException {
		this.channel = channel;
		this.con = channel.getConnection();
		this.env = env;
		this.terminal = terminal;
		this.reader = reader;
		this.shell = shell;
		this.fileFactory = getContext().getPolicy(FileSystemPolicy.class)
				.getFileFactory().getFileFactory(con);
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
	
	public void print(char ch) {
		terminal.writer().print(ch);
		terminal.writer().flush();
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
	
	public String readLine(String prompt, Character mask) {
		return reader.readLine(prompt, mask);
	}

	public Context getContext() {
		return con.getContext();
	}

	public String readLine() {
		return reader.readLine();
	}

	public SessionChannelServer getSessionChannel() {
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

	public void setCurrentDirectory(String currentDirectory) throws IOException, PermissionDeniedException {
		if(Objects.isNull(cwd)) {
			cwd = fileFactory.getFile((String)env.getOrDefault("HOME", "/"));
			if(!cwd.exists()) {
				cwd.createFolder();
				if(!cwd.exists()) {
					throw new FileNotFoundException(String.format("User directory %s does not exist", cwd.getName()));
				}
			} else {
				if(!cwd.isDirectory()) {
					throw new IOException(String.format("%s is not a directory", cwd.getName()));
				}
			}
		} 
		
		AbstractFile file = cwd.resolveFile(currentDirectory);
		
		if(!file.isDirectory()) {
			throw new IOException(String.format("%s is not a directory", file.getName()));
		}
		
		this.cwd = file;
		env.put("CWD", currentDirectory);
	}
	
	public AbstractFile getCurrentDirectory() throws IOException, PermissionDeniedException {
		if(Objects.isNull(cwd)) {
			setCurrentDirectory("");
		}
		return cwd;
	}

	public AbstractFileFactory<?> getFileFactory() {
		return fileFactory;
	}
	
	public static VirtualConsole getCurrentConsole() {
		return threadConsoles.get();
	}

	public static void setCurrentConsole(VirtualConsole console) {
		threadConsoles.set(console);
	}

	public static void clearCurrentConsole() {
		threadConsoles.remove();
	}
	
}
