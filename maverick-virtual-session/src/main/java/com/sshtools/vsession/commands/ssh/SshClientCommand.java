package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.vsession.ShellCommandWithOptions;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class SshClientCommand extends ShellCommandWithOptions {
	
	public SshClientCommand() {
		super("ssh", SUBSYSTEM_SHELL, "", "Returns the ssh client shell", SshClientOptions.getOptions());
	}
	
	@Override
	public String getUsage() {
		StringWriter out = new StringWriter();
		
		PrintWriter pw = new PrintWriter(out);
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( pw, formatter.getWidth(), "ssh", "", 
				getOptions(), formatter.getLeftPadding(), formatter.getDescPadding(), "" );
        pw.flush();
        
        String result = out.toString();
        
		return result;
	}


	@Override
	public void run(CommandLine cli, final VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		SshClientArguments arguments = SshClientOptionsEvaluator.evaluate(cli);
		
		SshClient sshClient = null;
		try {
			
			sshClient = new SshClient(arguments.getDestination(), arguments.getPort(), arguments.getLoginName());
			
			ClientAuthenticator auth; 
			do { 
			   auth = new PasswordAuthenticator(console.getLineReader().readLine("Password :", '\0'));
			   if(sshClient.authenticate(auth, 30000)) { 
			      break; 
			   } 
			} while(sshClient.isConnected());
			
			Connection<SshClientContext> connection = sshClient.getConnection();
			
			console.println("Starting new shell.");
			console.println();
			
			ShellTask shell = new ShellTask(connection) {

				protected void beforeStartShell(SessionChannelNG session) {
					
					session.allocatePseudoTerminal(console.getTerminal().getType(), 
							console.getTerminal().getWidth(), 
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

}
