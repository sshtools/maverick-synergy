
package com.sshtools.server.vsession.commands.sftp;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.server.vsession.AbstractCommand;

public abstract class SftpCommand extends AbstractCommand {

	protected SftpClient sftp;
	
	public SftpCommand(String name, String subsystem, String usage, String description) {
		super(name, subsystem, usage, description);
	}

	public void setSftpClient(SftpClient sftp) {
		this.sftp = sftp;
	}

}
