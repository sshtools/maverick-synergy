package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.VirtualConsole;

public class Sftp extends Msh {

	SftpClient sftp;
	
	@Override
	public void run(String[] args, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		/**
		 * TODO connect to SFTP server and provide a file system shell experience, just
		 * like the standard sftp command on Linux or OSX.
		 */
		//SshClient ssh = createSshClient(args);
		//this.sftp = new SftpClient(ssh, console.getFileFactory())
		
		Object previousPrompt = console.getEnvironment().put("PROMPT", "sftp> ");
		try {
			super.run(args, console);
		} finally {
			console.getEnvironment().put("PROMPT", previousPrompt);
		}
	}

	public Sftp() {
		super("sftp", "", "", "");
		setCommandFactory(new SftpCommandFactory());
	}

	private class SftpCommandFactory extends CommandFactory<SftpCommand> {
		
		@Override
		protected void configureCommand(SftpCommand command, SshConnection con)
				throws IOException, PermissionDeniedException {
			command.setSftpClient(sftp);
		}

		SftpCommandFactory() {
			installCommand(Quit.class);
			installCommand(Lpwd.class);
		}
	}
}
