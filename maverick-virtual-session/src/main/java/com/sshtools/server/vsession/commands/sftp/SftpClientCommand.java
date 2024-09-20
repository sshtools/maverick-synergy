package com.sshtools.server.vsession.commands.sftp;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandArgumentsParser;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.vsession.commands.ssh.SshClientArguments;
import com.sshtools.vsession.commands.ssh.SshClientHelper;

public class SftpClientCommand extends Msh {
	
	private Options options = new Options();

	public SftpClientCommand() {
		super("sftp", SUBSYSTEM_SHELL, "", "Returns the sftp client shell");
		for (Option option : SftpClientOptions.getOptions()) {
			this.options.addOption(option);
		}
	}

	public Options getOptions() {
		return options;
	}

	@Override
	public String getUsage() {
		StringWriter out = new StringWriter();

		PrintWriter pw = new PrintWriter(out);
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(pw, formatter.getWidth(), "sftp", "", getOptions(), formatter.getLeftPadding(),
				formatter.getDescPadding(), "");
		pw.flush();

		String result = out.toString();

		return result;
	}
	
	@Override
	public void run(String[] args, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		String[] argsCopy = new String[args.length];
		System.arraycopy(args, 0, argsCopy, 0, args.length);
		
		CommandLine commandLine = CommandArgumentsParser.parse(getOptions(), argsCopy, getUsage());
		
		SshClientArguments arguments = SftpClientOptionsEvaluator.evaluate(commandLine, console);


		SshClient sshClient = null;
		try {
			
			
			sshClient = SshClientHelper.connectClient(arguments, console);
			
			Connection<SshClientContext> connection = sshClient.getConnection();

			var sftp = SftpClientBuilder.create().
					withConnection(connection).
					withFileFactory(console.getFileFactory()).build();
			
			Object previousPrompt = console.getEnvironment().put("PROMPT", "sftp> ");
			setCommandFactory(new SftpCommandFactory(sftp));
			
			try {
				runShell(console);
			} finally {
				console.getEnvironment().put("PROMPT", previousPrompt);
			}
	
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			if (sshClient != null) {
				sshClient.close();
			}
		}

	}
	
	class SftpCommandFactory extends CommandFactory<SftpCommand> {
		
		SftpClient sftpClient;
		
		SftpCommandFactory(SftpClient sftpClient) {
			this.sftpClient = sftpClient;
			installCommand(Quit.class);
			installCommand(Lpwd.class);
			installCommand(Pwd.class);
			installCommand(Cd.class);
			installCommand(Lcd.class);
			installCommand(Ls.class);
			installCommand(Put.class);
			installCommand(Get.class);
			installCommand(Chgrp.class);
			installCommand(Chmod.class);
			installCommand(Chown.class);
			installCommand(Mkdir.class);
			installCommand(Rename.class);
			installCommand(Rm.class);
			installCommand(Rmdir.class);
		}
		
		@Override
		protected void configureCommand(SftpCommand command, SshConnection con)
				throws IOException, PermissionDeniedException {
			command.setSftpClient(this.sftpClient);
		}
	}

}
