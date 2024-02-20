package com.sshtools.server.vsession;

import java.util.List;

import com.sshtools.common.ssh.SshConnection;

public abstract class AbstractUUIDCommand extends ShellCommand {

	public AbstractUUIDCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}

	public int complete(String buffer, int cursor, List<String> candidates, VirtualConsole console) {
		
		for(SshConnection c : console.getConnection().getConnectionManager().getAllConnections()) {
			if(buffer==null || c.getUUID().startsWith(buffer)) {
				candidates.add(c.getUUID());
			}
		}
		return 0;
	}

}
