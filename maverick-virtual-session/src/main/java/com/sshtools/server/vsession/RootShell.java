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
package com.sshtools.server.vsession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.SessionChannelHelper;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.Utils;
import com.sshtools.server.vsession.CmdLine.Condition;

public class RootShell extends Msh {

	private List<CmdLine> commands;
	private VirtualConsole console;

	public RootShell(CommandFactory<ShellCommand> commandFactory,
			SshConnection con) throws PermissionDeniedException,
			IOException {
		super(commandFactory);
	}

	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException {
		
		this.console = console;
		VirtualConsole.setCurrentConsole(console);
		
		try {
			if (commands != null) {
				runAndExit(commands, args, console);
			} else {
				promptForCommands(args, console);
			}
		} finally {
			VirtualConsole.clearCurrentConsole();
		}
	}

	protected void promptForCommands(String[] args, VirtualConsole console) throws IOException, PermissionDeniedException {
		
		writeWelcome();
		
		super.run(args, console);
		
		console.getSessionChannel().close();
	}
	

	private void writeWelcome() {
		
		String welcomeText = console.getContext().getPolicy(VirtualSessionPolicy.class).getWelcomeText();
		
		if(Utils.isNotBlank(welcomeText)) {
			try {
				welcomeText = welcomeText.replace("${hostname}",
						java.net.InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				welcomeText=  welcomeText.replaceAll("${hostname}", "localhost");
			}
			
			// Just in case its not set in the configuration we set it
			welcomeText = welcomeText.replace("${productName}", "Virtual SSHD");
			welcomeText = welcomeText.replace("${username}", console.getConnection().getUsername());
			
			
			if(welcomeText.contains("${remote")) {
				welcomeText = welcomeText.replace("${remoteAddress}",
						console.getConnection().getRemoteAddress().getHostAddress());
				welcomeText = welcomeText.replace("${remoteHost}",
						console.getConnection().getRemoteAddress().getHostName());
				welcomeText = welcomeText.replace("${remotePort}", String
						.valueOf(console.getConnection().getRemotePort()));
			}
			

			if(welcomeText.contains("${local")) {
				welcomeText = welcomeText.replace("${localAddress}",
						console.getConnection().getLocalAddress().getHostAddress());
				welcomeText = welcomeText.replace("${localHost}",
						console.getConnection().getLocalAddress().getHostName());
				welcomeText = welcomeText.replace("${localPort}", String
						.valueOf(console.getConnection().getLocalPort()));
			}
			
			
			SimpleDateFormat df = new SimpleDateFormat("d MMM, yyyy");
			SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
			Date date = new Date();
			welcomeText = welcomeText.replace("${version}", console.getConnection().getServerVersion());
			welcomeText = welcomeText.replace("${date}", df.format(date));
			welcomeText = welcomeText.replace("${time}", tf.format(date));
			welcomeText = welcomeText.replace("${year}", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
	
			if (Utils.isNotBlank(welcomeText)) {
				console.println(welcomeText);
			}
		}
		
	}

	protected void runAndExit(List<CmdLine> commands, String[] args, VirtualConsole console) throws IOException {
		
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
			l.setExitCode(exitCode = spawn(console,
					l.getArgArray(), l.isBackground()));
		}
		if (exitCode == ShellCommand.STILL_ACTIVE)
			exitCode = 0;
		
		SessionChannelHelper.sendExitStatus(console.getSessionChannel(), lastCommand.getExitCode());
		console.getSessionChannel().close();
		
	}

	public boolean startShell(InputStream in, VirtualConsole console) throws IOException,
			PermissionDeniedException {

		this.console = console;
		
		// Run any profile resources
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = getClass().getClassLoader();
		}
		for (Enumeration<URL> en = cl.getResources("META-INF/default-profile"); en
				.hasMoreElements();) {
			URL url = en.nextElement();
			source(console, url.openStream());
		}

		// Home profile
		AbstractFile homeProfile = console.getCurrentDirectory().resolveFile(".profile");
		if (homeProfile.exists() && homeProfile.isReadable()) {
			source(console, homeProfile);
		}

		// Fire event
		EventServiceImplementation.getInstance().fireEvent(
				new Event(this, EventCodes.EVENT_ROOT_SHELL_STARTED,
						true)
						.addAttribute(EventCodes.ATTRIBUTE_FILE_FACTORY,
								console.getCurrentDirectory().getFileFactory())
						.addAttribute(EventCodes.ATTRIBUTE_CONNECTION,
								console.getConnection()));


		console.getSessionChannel().addEventListener(new ChannelEventListener() {
				@Override
				public void onChannelClose(Channel channel) {

					EventServiceImplementation
							.getInstance()
							.fireEvent(
									new Event(
											this,
											EventCodes.EVENT_ROOT_SHELL_STOPPED,
											true)
											.addAttribute(
													EventCodes.ATTRIBUTE_FILE_FACTORY,
													console.getFileFactory())
											.addAttribute(
													EventCodes.ATTRIBUTE_CONNECTION,
													console.getConnection()));

				}
			});

		return true;
	}

//	protected void setKeyBindings(InputStream keyBindings) throws IOException {
//		if (keyBindings == null) {
//			keyBindings = console.getTerminal().
//					.getDefaultBindings();
//		}
//		console.getConsole().setKeyBindings(keyBindings);
//	}

	public void exit() {
		super.exit();
		console.getSessionChannel().close();
	}

	public void execCommand(InputStream in, VirtualConsole console, String cmd) throws IOException,
			PermissionDeniedException {
		startShell(in, console);
		LineParser lineParser = new LineParser(console.getEnvironment());
		commands = lineParser.parseCommands(cmd, 0);
	}

	public VirtualConsole getProcess() {
		return console;
	}

	public void start() {
		console.getConnection().executeTask(new Runnable() {
			public void run() {

				try {
					RootShell.this.run(new String[] { getCommandName()}, console);
				} catch (Exception e) {
					Log.error("Failed to start shell.", e);
				} finally {
					console.destroy();
				}
			}
		});
	}
}
