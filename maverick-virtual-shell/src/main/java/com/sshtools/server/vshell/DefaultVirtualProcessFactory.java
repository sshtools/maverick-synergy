package com.sshtools.server.vshell;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.server.vshell.terminal.Console;
import com.sshtools.server.vshell.terminal.TerminalOutput;

public class DefaultVirtualProcessFactory implements VirtualProcessFactory {

	Set<VirtualProcess> rootProcesses = new HashSet<VirtualProcess>();

	public VirtualProcess createRootProcess(TerminalOutput terminal, Msh msh,
			Environment environment, Thread thread, ShellCommand command,
			AbstractFile workingDirectory, Console console,
			SessionChannelServer session) throws IOException,
			PermissionDeniedException {
		VirtualProcess process = new VirtualProcess(terminal, msh, environment,
				thread, command, workingDirectory, null, console, session, this);
		rootProcesses.add(process);
		return process;
	}

	public VirtualProcess createChildProcess(VirtualProcess parent,
			ShellCommand cmd, Msh msh) throws IOException,
			PermissionDeniedException {
		return new VirtualProcess(parent, cmd, msh, this);
	}

	public Collection<VirtualProcess> getRootProcesses() {
		return rootProcesses;
	}

	public void destroy(VirtualProcess virtualProcess) {
		rootProcesses.remove(virtualProcess);
	}

}
