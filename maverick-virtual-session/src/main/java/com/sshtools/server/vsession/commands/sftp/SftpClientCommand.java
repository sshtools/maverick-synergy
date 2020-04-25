/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.vsession.CommandArgumentsParser;
import com.sshtools.server.vsession.CommandFactory;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.vsession.commands.ssh.CommandUtil;

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
			
			SshClientContext context = getSshContext(sftpClientArguments);
			setUpCipherSpecs(sftpClientArguments, context);
			setUpCompression(sftpClientArguments, context);
			
			sshClient = new SshClient(destination, sftpClientArguments.getPort(), user, context);
			
			ClientAuthenticator auth;

			if (CommandUtil.isNotEmpty(sftpClientArguments.getIdentityFile())) {
				
				String identityFile = sftpClientArguments.getIdentityFile();
				AbstractFile identityFileTarget = console.getCurrentDirectory().resolveFile(identityFile);
				SshPrivateKeyFile pkf = SshPrivateKeyFileFactory.parse(identityFileTarget.getInputStream());
				
				String passphrase = null;
				if (pkf.isPassphraseProtected()) {
					do {
						passphrase = console.getLineReader().readLine("Passphrase :", '\0');
						SshKeyPair pair = pkf.toKeyPair(passphrase);

						auth = new PublicKeyAuthenticator(pair);

						if (sshClient.authenticate(auth, 30000)) {
							break;
						}
					} while (sshClient.isConnected());
				}

			} else {

				do {
					auth = new PasswordAuthenticator(console.getLineReader().readLine("Password :", '\0'));
					if (sshClient.authenticate(auth, 30000)) {
						break;
					}
				} while (sshClient.isConnected());
			}
			

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
	
	private SshClientContext getSshContext(SftpClientArguments arguments) throws IOException, SshException {
		if (CommandUtil.isNotEmpty(arguments.getCiphers())) {
			return new SshClientContext(SecurityLevel.NONE);
		}
				
		return new SshClientContext();
	}
	
	private void setUpCipherSpecs(SftpClientArguments arguments, SshClientContext ctx)
			throws IOException, SshException {
		if (CommandUtil.isNotEmpty(arguments.getCiphers())) {
			String[] cipherSpecs = arguments.getCiphers();
			
			for (int i = cipherSpecs.length - 1; i >= 0; --i) {
				ctx.setPreferredCipherCS(cipherSpecs[i]); 
				ctx.setPreferredCipherSC(cipherSpecs[i]);
			}
			
		}
	}
	
	private void setUpCompression(SftpClientArguments arguments, SshClientContext ctx) 
			throws IOException, SshException {
		if (arguments.isCompression()) {
			ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB);
 			ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB);
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
