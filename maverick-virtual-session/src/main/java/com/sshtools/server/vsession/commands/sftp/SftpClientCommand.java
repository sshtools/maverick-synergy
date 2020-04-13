package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.CommandArgumentsParser;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.VirtualConsole;

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
		
		SftpClientArguments sftpClientArguments = SftpClientOptionsEvaluator.evaluate(commandLine);
		

		SshClient sshClient = null;
		try {
			
			String[] userAndDestination = SftpClientOptionsEvaluator.extractUserAndDestination(commandLine.getArgList());
			String user = userAndDestination[0];
			String destination = userAndDestination[1];
			
			sshClient = new SshClient(destination, sftpClientArguments.getPort(), user, new SshClientContext());
			ClientAuthenticator auth;

			do {
				auth = new PasswordAuthenticator(console.getLineReader().readLine("Password :", '\0'));
				if (sshClient.authenticate(auth, 30000)) {
					break;
				}
			} while (sshClient.isConnected());
			

			Connection<SshClientContext> connection = sshClient.getConnection();

			SftpClient sftp = new SftpClient(connection, console.getFileFactory());
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
			installCommand(Ls.class);
			installCommand(Put.class);
		}
		
		@Override
		protected void configureCommand(SftpCommand command, SshConnection con)
				throws IOException, PermissionDeniedException {
			command.setSftpClient(this.sftpClient);
		}
	}

}
