package com.sshtools.server.vshell;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.server.vshell.terminal.Console;
import com.sshtools.server.vshell.terminal.TerminalOutput;

public interface VirtualProcessFactory {

	VirtualProcess createRootProcess(TerminalOutput terminal,
			Msh msh,
			Environment environment, 
			Thread thread, 
			ShellCommand command,
			AbstractFile workingDirectory, 
			Console console, 
			SessionChannelServer session) throws IOException,
			PermissionDeniedException;

	VirtualProcess createChildProcess(VirtualProcess parent,
			ShellCommand command, Msh msh) throws IOException,
			PermissionDeniedException;
	
	Collection<VirtualProcess> getRootProcesses();

	void destroy(VirtualProcess virtualProcess);
}
