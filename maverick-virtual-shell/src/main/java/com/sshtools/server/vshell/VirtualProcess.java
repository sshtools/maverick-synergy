package com.sshtools.server.vshell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.terminal.Console;
import com.sshtools.server.vshell.terminal.TerminalOutput;

public class VirtualProcess {

	private Environment environment;
	private Thread thread;
	private ShellCommand command;
	private AbstractFile workingDirectory;
	private VirtualProcess parent;
	private List<VirtualProcess> children = new ArrayList<VirtualProcess>();
	private boolean destroyed;
	private Console console;
	private final TerminalOutput terminal;
	private SshConnection connection;
	private SessionChannelServer session;
	private Msh msh;
	private VirtualProcessFactory processFactory;

	VirtualProcess(VirtualProcess parent, ShellCommand command, Msh msh, VirtualProcessFactory processFactory)
			throws IOException, PermissionDeniedException {
		this(parent.getTerminal(), msh,
				new Environment(parent.getEnvironment()), Thread
						.currentThread(), command,
				parent.getCurrentDirectory(), parent, parent.getConsole(),
				parent.getSessionChannel(),
				processFactory);
	}

	VirtualProcess(TerminalOutput terminal, Msh msh, Environment environment,
			Thread thread, ShellCommand command, AbstractFile workingDirectory,
			VirtualProcess parent, Console console, SessionChannelServer session, VirtualProcessFactory processFactory)
			throws IOException, PermissionDeniedException {
		super();
		this.terminal = terminal;
		this.connection = session.getConnection();
		this.console = console;
		this.environment = environment;
		this.thread = thread;
		this.command = command;
		this.parent = parent;
		this.session = session;
		this.msh = msh;
		this.processFactory = processFactory;

		if (parent != null) {
			parent.addChild(this);
		}

		setCurrentDirectory(workingDirectory);

		environment.put("PID", String.valueOf(getPID()));
	}
	
	public VirtualProcessFactory getProcessFactory() {
		return processFactory;
	}

	public TerminalOutput getTerminal() {
		return terminal;
	}

	public SshConnection getConnection() {
		return connection;
	}

	public Context getContext() {
		return connection.getContext();
	}

	public SessionChannelServer getSessionChannel() {
		return session;
	}

	public Console getConsole() {
		return console;
	}

	public Msh getMsh() {
		return msh;
	}
	
	public boolean killProcess(long pid) {
		VirtualProcess actual = getRootProcess().findProcess(pid);
		if (actual != null) {
			actual.getThread().interrupt();
			return true;
		}
		return false;
	}

	public VirtualProcess getRootProcess() {
		checkState();
		VirtualProcess process = this;
		while (process.getParent() != null) {
			process = process.getParent();
		}
		return process;
	}

	public void destroy() {
		checkState();
		if (parent != null) {
			parent.removeChild(this);
		} else {
			processFactory.destroy(this);
		}
		destroyed = true;
	}

	protected void removeChild(VirtualProcess process) {
		checkState();
		children.remove(process);
	}

	protected void addChild(VirtualProcess process) {
		checkState();
		children.add(process);
	}

	public VirtualProcess getParent() {
		checkState();
		return parent;
	}

	public void setParent(VirtualProcess parent) {
		checkState();
		this.parent = parent;
	}

	public Environment getEnvironment() {
		checkState();
		return environment;
	}

	public void setEnvironment(Environment environment) {
		checkState();
		this.environment = environment;
	}

	public Thread getThread() {
		checkState();
		return thread;
	}

	public void setThread(Thread thread) {
		checkState();
		this.thread = thread;
	}

	public ShellCommand getCommand() {
		checkState();
		return command;
	}

	public void setCommand(ShellCommand command) {
		checkState();
		this.command = command;
	}

	public AbstractFile getCurrentDirectory() {
		checkState();
		return workingDirectory;
	}

	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return workingDirectory.getFileFactory();
	}

	private void checkState() {
		if (destroyed) {
			throw new IllegalStateException("Process destroyed.");
		}
	}

	public void setCurrentDirectory(String workingDirectory)
			throws IOException, PermissionDeniedException {
		setCurrentDirectory(getCurrentDirectory().resolveFile(workingDirectory));
	}

	public void setCurrentDirectory(AbstractFile workingDirectory)
			throws IOException, PermissionDeniedException {
		if (!workingDirectory.exists()) {
			workingDirectory.createFolder();
			workingDirectory.refresh();
		}
		if (!workingDirectory.exists() || !workingDirectory.isDirectory()) {
			throw new IOException(workingDirectory.getName()
					+ " does not exist or is not a directory");
		}
		this.workingDirectory = workingDirectory;
		environment.put("CWD", workingDirectory.getAbsolutePath());
	}

	public Collection<VirtualProcess> getChildren() {
		return children;
	}

	public VirtualProcess findProcess(long pid) {
		if (this.hashCode() == pid) {
			return this;
		}
		for (VirtualProcess c : children) {
			VirtualProcess p = c.findProcess(pid);
			if (p != null) {
				return p;
			}
		}
		return null;
	}

	public int getPID() {
		return hashCode();
	}
}
