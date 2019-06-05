package com.sshtools.server.vshell.commands;

import java.util.List;

import org.apache.commons.cli.Option;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public abstract class AbstractUUIDCommand<T extends AbstractFile> extends ShellCommand {

	public AbstractUUIDCommand(String name, String subsystem) {
		super(name, subsystem);
	}

	public AbstractUUIDCommand(String name, String subsystem, String signature) {
		super(name, subsystem, signature);
	}

	public AbstractUUIDCommand(String name, String subsystem, String signature,
			Option... options) {
		super(name, subsystem, signature, options);
	}

	public int complete(String buffer, int cursor, List<String> candidates, VirtualProcess process) {
		
		for(SshConnection c : process.getConnection().getConnectionManager().getAllConnections()) {
			if(buffer==null || c.getUUID().startsWith(buffer)) {
				candidates.add(c.getUUID());
			}
		}
		return 0;
	}

}