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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.lang.StringUtils;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.ChannelEventAdapter;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SessionChannelHelper;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.ProcessUtils;
import com.sshtools.server.vshell.CmdLine.Condition;
import com.sshtools.server.vshell.terminal.Console;
import com.sshtools.server.vshell.terminal.TerminalOutput;

import jline.Terminal;

public class RootShell extends Msh {

	//
	// Some additional event attributes
	//
	public static final String ATTRIBUTE_PROCESS = "PROCESS";

	private List<CmdLine> commands;
	private AbstractFile homeDir;
	private SshConnection con;
	private String welcomeText;
	public static final String ROOT_PROCESSES = "rootProcesses";

	public RootShell(CommandFactory<ShellCommand> commandFactory,
			SshConnection con,
			String welcomeText) throws PermissionDeniedException,
			IOException {
		super(commandFactory);
		this.con = con; 
		this.welcomeText = welcomeText;
		this.homeDir = con.getContext().getFileFactory().getDefaultPath(con);
	}

	public SshConnection getConnection() {
		return con;
	}

	public void run(CommandLine args, VirtualProcess process)
			throws IOException, PermissionDeniedException {
		if (commands != null) {
			runAndExit(commands, args, process);
		} else {
			promptForCommands(args, process);
		}
	}

	protected void promptForCommands(CommandLine args, VirtualProcess process) throws IOException, PermissionDeniedException {
		
		Console console = process.getConsole();
		
		if(StringUtils.isNotBlank(welcomeText)) {
			welcomeText = welcomeText.replace("${hostname}",
					java.net.InetAddress.getLocalHost().getHostName());
			// Just in case its not set in the configuration we set it
			welcomeText = welcomeText.replace("${ProductName}", "Virtual SSHD");
			welcomeText = welcomeText.replace("${username}", process
					.getConnection().getUsername());
			
			
			if(welcomeText.contains("${remote")) {
				welcomeText = welcomeText.replace("${remoteAddress}",
						process.getConnection().getRemoteAddress().getHostAddress());
				welcomeText = welcomeText.replace("${remoteHost}",
						process.getConnection().getRemoteAddress().getHostName());
				welcomeText = welcomeText.replace("${remotePort}", String
						.valueOf(process.getConnection().getRemotePort()));
			}
			

			if(welcomeText.contains("${local")) {
				welcomeText = welcomeText.replace("${localAddress}",
						process.getConnection().getLocalAddress().getHostAddress());
				welcomeText = welcomeText.replace("${localHost}",
						process.getConnection().getLocalAddress().getHostName());
				welcomeText = welcomeText.replace("${localPort}", String
						.valueOf(process.getConnection().getLocalPort()));
			}
			
			
			SimpleDateFormat df = new SimpleDateFormat("d MMM, yyyy");
			SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
			Date date = new Date();
			welcomeText = welcomeText.replace("${version}", con.getServerVersion());
			welcomeText = welcomeText.replace("${date}", df.format(date));
			welcomeText = welcomeText.replace("${time}", tf.format(date));
			welcomeText = welcomeText.replace("${year}", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
	
			if (StringUtils.isNotBlank(welcomeText)) {
				console.printStringNewline(welcomeText);
				console.printNewline();
			}
		}
		super.run(args, process);
		process.getSessionChannel().close();
	}

	protected void runAndExit(List<CmdLine> commands, CommandLine args, VirtualProcess process) throws IOException {
		
		int exitCode = 0;
		CmdLine lastCommand = null;
		boolean exit = false;

		for (CmdLine l : commands) {

			if (exit)
				break;

			exitCode = (lastCommand == null ? 0 : lastCommand.getExitCode());
			switch (exitCode) {
			case 0:
			case STILL_ACTIVE:
				if (lastCommand != null
						&& lastCommand.getCondition() == Condition.ExecNextCommandOnFailure) {
					exit = true;
					continue;
				}
				break;
			default:
				if (lastCommand != null
						&& lastCommand.getCondition() == Condition.ExecNextCommandOnSuccess) {
					exit = true;
					continue;
				}
				break;
			}
			lastCommand = l;
			l.setExitCode(exitCode = spawn(process.getConsole(), process,
					l.getArgArray(), l.isBackground()));
		}
		if (exitCode == ShellCommand.STILL_ACTIVE)
			exitCode = 0;
		
		SessionChannelHelper.sendExitStatus(process.getSessionChannel(), process.getCommand().getExitCode());
		process.getSessionChannel().close();
		
	}

	public boolean startShell(InputStream in, TerminalOutput term,
			SessionChannelServer session) throws IOException,
			PermissionDeniedException {

		Console console = new Console(in,
				term.getAttachedOutputStream(), null, new InteractiveTerminal(
						term), getCommandFactory());
		Environment environment = createEnvironment(session);

		VirtualProcessFactory processFactory;
		
		if(con.getContext().hasPolicy(VirtualProcessFactory.class)) {
			processFactory = con.getContext().getPolicy(VirtualProcessFactory.class);
		} else {
			processFactory = con.getContext().getPolicy(DefaultVirtualProcessFactory.class);
		}

		process = processFactory.createRootProcess(term,
				this, environment, Thread.currentThread(), this, homeDir,
				console, session);

		process.getEnvironment().put("HOME", homeDir.getAbsolutePath());

		console.init(process);

		// Run any profile resources
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = getClass().getClassLoader();
		}
		for (Enumeration<URL> en = cl.getResources("META-INF/default-profile"); en
				.hasMoreElements();) {
			URL url = en.nextElement();
			source(process, url.openStream());
		}

		// Home profile
		AbstractFile homeProfile = homeDir.resolveFile(".profile");
		if (homeProfile.exists() && homeProfile.isReadable()) {
			source(process, homeProfile);
		}

		// Load history
		AbstractFile history = homeDir.resolveFile(".history");
		if (history.exists()) {
			InputStream inputStream = history.getInputStream();
			try {
				process.getConsole().getHistory().load(inputStream);
			} finally {
				inputStream.close();
			}
		}

		// Fire event
		EventServiceImplementation.getInstance().fireEvent(
				new Event(this, EventCodes.EVENT_ROOT_SHELL_STARTED,
						true)
						.addAttribute(ATTRIBUTE_PROCESS, process)
						.addAttribute(EventCodes.ATTRIBUTE_NFS,
								homeDir.getFileFactory())
						.addAttribute(EventCodes.ATTRIBUTE_CONNECTION,
								process.getConnection()));

		try {

			final PrintWriter output = new PrintWriter(
					history.getOutputStream(true));
			process.getConsole().getHistory().setOutput(output);
			session.addEventListener(new ChannelEventAdapter() {
				@Override
				public void onChannelClose(Channel channel) {

					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(
											this,
											EventCodes.EVENT_ROOT_SHELL_STOPPED,
											true)
											.addAttribute(ATTRIBUTE_PROCESS,
													process)
											.addAttribute(
													EventCodes.ATTRIBUTE_NFS,
													homeDir.getFileFactory())
											.addAttribute(
													EventCodes.ATTRIBUTE_CONNECTION,
													process.getConnection()));

					output.flush();
					output.close();

				}
			});
		} catch (IOException fse) {
			Log.warn("No history. " + fse.getMessage());
		}

		return true;
	}

	protected void setKeyBindings(InputStream keyBindings) throws IOException {
		if (keyBindings == null) {
			keyBindings = process.getConsole().getTerminal()
					.getDefaultBindings();
		}
		process.getConsole().setKeyBindings(keyBindings);
	}

	public void exit() {
		super.exit();
		process.getSessionChannel().close();
	}

	private Environment createEnvironment(SessionChannelServer session) {
		Environment environment = new Environment();
		environment.put("USERNAME", session.getConnection().getUsername());
		environment.put("CONNECTION_ID", session.getConnection().getSessionId());
		
		try {
			environment.put("HOSTNAME", ProcessUtils.executeCommand("hostname"));
		} catch(IOException ex) {
			if(!System.getenv().containsKey("HOSTNAME")) {
				try {
					environment.put("HOSTNAME", InetAddress.getLocalHost().getHostName());
				} catch (UnknownHostException e) {
				}
			} else {
				environment.put("HOSTNAME", System.getenv().get("HOSTNAME"));
			}
		}
		environment.put("REMOTE_ADDRESS", session.getConnection()
				.getRemoteAddress().toString());
		environment.put("PROMPT", "# ");
		return environment;
	}

	public void execCommand(InputStream in, TerminalOutput term,
			String cmd, SessionChannelServer session) throws IOException,
			PermissionDeniedException {
		startShell(in, term, session);
		LineParser lineParser = new LineParser(process.getEnvironment());
		commands = lineParser.parseCommands(cmd, 0);
	}

	public VirtualProcess getProcess() {
		return process;
	}

	class InteractiveTerminal extends Terminal {

		private boolean echo = true;
		private TerminalOutput terminalIO;

		public InteractiveTerminal(TerminalOutput terminalIO)
				throws IOException {
			this.terminalIO = terminalIO;
			try {
				initializeTerminal();
			} catch (Exception e) {
				IOException ioe = new IOException(
						"Failed to initialise terminal.");
				ioe.initCause(e);
				throw ioe;
			}
		}

		@Override
		public void disableEcho() {
			this.echo = false;
		}

		@Override
		public void enableEcho() {
			this.echo = true;
		}

		@Override
		public boolean isANSISupported() {
			return true;
		}

		@Override
		public boolean getEcho() {
			return false;
		}

		@Override
		public int getTerminalHeight() {
			return terminalIO.getRows();
		}

		@Override
		public int getTerminalWidth() {
			return terminalIO.getCols();
		}

		@Override
		public void initializeTerminal() throws Exception {
			echo = true;
		}

		@Override
		public boolean isEchoEnabled() {
			return echo;
		}

		@Override
		public boolean isSupported() {
			return true;
		}
	}

	public void start() {
		con.executeTask(new Runnable() {
			public void run() {
				DefaultParser parse = new DefaultParser();
				try {
					CommandLine cl = parse.parse(getOptions(),
							new String[] { getCommandName() });

					RootShell.this.run(cl, process);
				} catch (Exception e) {
					Log.error("Failed to start root shell.", e);
				} finally {
					process.destroy();
				}
			}
		});
	}
}
