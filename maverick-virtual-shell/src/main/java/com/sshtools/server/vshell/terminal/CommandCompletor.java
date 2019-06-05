package com.sshtools.server.vshell.terminal;

import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.Command;
import com.sshtools.server.vshell.CommandFactory;
import com.sshtools.server.vshell.VirtualProcess;

import jline.Completor;

public class CommandCompletor<T extends Command> implements Completor {
	final 
	public static ThreadLocal<Command> command = new ThreadLocal<Command>();

	private CommandFactory<T> commandFactory;
	private SshConnection con;
	private VirtualProcess process;

	public CommandCompletor(VirtualProcess process,
			CommandFactory<T> commandFactory, SshConnection con) {
		this.commandFactory = commandFactory;
		this.process = process;
	}

	public int complete(String buffer, int cursor, List<String> candidates) {
		// System.out.println("Buffer: " + buffer + " Cursor: " + cursor);
		for (String cmd : commandFactory.getSupportedCommands()) {
			if (buffer != null && buffer.equals(cmd)) {
				try {
					Command c = commandFactory.createCommand(cmd, con);
					c.init(process);
					command.set(c);
				} catch (Exception e) {
					Log.error("Failed to load command for completion.", e);
				}
				candidates.add(cmd);
			} else if (buffer == null || cmd.startsWith(buffer.trim())) {
				candidates.add(cmd);
			}
		}
		return 0;
	}
}
