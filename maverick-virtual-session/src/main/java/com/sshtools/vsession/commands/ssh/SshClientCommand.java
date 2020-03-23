package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.ShellCommandWithOptions;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class SshClientCommand extends ShellCommandWithOptions {
	
	private String[] originalArguments = null;

	public SshClientCommand() {
		super("ssh", SUBSYSTEM_SHELL, "", "Returns the ssh client shell", SshClientOptions.getOptions());
	}

	@Override
	public String getUsage() {
		StringWriter out = new StringWriter();

		PrintWriter pw = new PrintWriter(out);
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(pw, formatter.getWidth(), "ssh", "", getOptions(), formatter.getLeftPadding(),
				formatter.getDescPadding(), "");
		pw.flush();

		String result = out.toString();

		return result;
	}

	@Override
	public void run(CommandLine cli, final VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		SshClientArguments arguments = SshClientOptionsEvaluator.evaluate(cli, this.originalArguments);
		
		if (Log.isDebugEnabled()) {
			Log.debug(String.format("The arguments parsed are %s", arguments));
		}
		

		SshClient sshClient = null;
		try {
			
			sshClient = new SshClient(arguments.getDestination(), arguments.getPort(), arguments.getLoginName());
			ClientAuthenticator auth;

			if (CommandUtil.isNotEmpty(arguments.getIdentityFile())) {
				
				String identityFile = arguments.getIdentityFile();
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
			
			if (CommandUtil.isNotEmpty(arguments.getCommand())) {
				String command = arguments.getCommand();
				String result = sshClient.executeCommand(command);
				console.println(result);
				return;
			}

			Connection<SshClientContext> connection = sshClient.getConnection();

			console.println("Starting new shell.");
			console.println();

			ShellTask shell = new ShellTask(connection) {

				protected void beforeStartShell(SessionChannelNG session) {

					session.allocatePseudoTerminal(console.getTerminal().getType(), console.getTerminal().getWidth(),
							console.getTerminal().getHeight());
				}

				@Override
				protected void onOpenSession(final SessionChannelNG session)
						throws IOException, SshException, ShellTimeoutException {

					console.getSessionChannel().enableRawMode();

					con.addTask(new ConnectionAwareTask(con) {
						@Override
						protected void doTask() throws Throwable {
							IOUtils.copy(console.getSessionChannel().getInputStream(), session.getOutputStream());
						}
					});
					IOUtils.copy(session.getInputStream(), console.getSessionChannel().getOutputStream());
				}

			};

			connection.addTask(shell);
			shell.waitForever();

			console.getSessionChannel().disableRawMode();
			console.println();
			console.println("Shell closed.");

		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			if (sshClient != null) {
				sshClient.close();
			}
		}

	}
	
	@Override
	protected String[] filterArgs(String[] args) {
		this.originalArguments = args;
		int indexTillSshClientCommandFound = CommandUtil.extractSshCommandLineFromExecuteCommand(args);
		return Arrays.copyOfRange(args, 0, indexTillSshClientCommandFound + 1);
	}
	
	

}
