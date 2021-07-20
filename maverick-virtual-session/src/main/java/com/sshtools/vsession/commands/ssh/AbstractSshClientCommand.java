
package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.sshtools.client.SshClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CommandArgumentsParser;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public abstract class AbstractSshClientCommand extends ShellCommand {
	
	private String[] originalArguments = null;
	protected Options options = new Options();
	protected VirtualConsole console;
	
	public AbstractSshClientCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}
	
	public Options getOptions() {
		return options;
	}

	@Override
	public String getUsage() {
		StringWriter out = new StringWriter();

		PrintWriter pw = new PrintWriter(out);
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(pw, formatter.getWidth(), 
				"ssh",
				"", 
				getOptions(), 
				formatter.getLeftPadding(),
				formatter.getDescPadding(), "");
		pw.flush();

		String result = out.toString();

		return result;
	}
	
	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		this.console = console;
		
		String[] filteredArgs = filterArgs(args);
		CommandLine cli = CommandArgumentsParser.parse(getOptions(), filteredArgs, getUsage());
		
		SshClientArguments arguments = generateCommandArguments(cli, this.originalArguments);
		
		if (Log.isDebugEnabled()) {
			Log.debug("The arguments parsed are {}", arguments);
		}
		
		SshClient sshClient = null;
		try {
			
			sshClient = SshClientHelper.connectClient(arguments, console);
			
			runCommand(sshClient, arguments, console);
			
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			if (sshClient != null) {
				sshClient.close();
			}
		}
	}

	protected abstract void runCommand(SshClient sshClient, SshClientArguments arguments, VirtualConsole console);

	protected abstract SshClientArguments generateCommandArguments(CommandLine cli, String[] args) throws IOException, PermissionDeniedException;

	protected String[] filterArgs(String[] args) {
		this.originalArguments = args;
		int indexTillSshClientCommandFound = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(args);
		return Arrays.copyOfRange(args, 0, indexTillSshClientCommandFound + 1);
	}

}
