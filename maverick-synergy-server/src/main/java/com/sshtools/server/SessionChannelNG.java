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

package com.sshtools.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.IdleStateListener;
import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ChannelOutputStream;
import com.sshtools.common.ssh.SessionChannelHelper;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.Utils;

/**
 * <p>
 * This class provides an abstract session, it handles all the requests defined
 * in the SSH Connection protocol for a session channel and passes the request
 * to the concrete implementation through its abstract methods.
 * </p>
 * 
 * <p>
 * When creating a session implementation your {@link #processStdinData(byte[])}
 * will receive data from the client and process as session input. To send
 * stdout data for your session you should use the
 * {@link #sendStdoutData(byte[])} method and any sdterr output should be send
 * using {@link #sendStderrData(byte[])} method.
 * </p>
 * 
 * <p>
 * As the server uses an asynchronous framework expensive blocking operations
 * SHOULD NOT be performed within your session as this will cause a deadlock on
 * the server.
 * </p>
 * 
 * <p>
 * The basic process of establishing a session is this
 * <ul>
 * <li>1. The session is opened</li>
 * <br>
 * <li>2. The client possibly sends requests for a pseudo terminal or setting of
 * environment variables</li>
 * <br>
 * <li>3. The client requests to either start a shell or execute a command. Once
 * this has been requested the interactive session starts and data can be sent
 * or received.</li>
 * <br>
 * <li>4. Data is sent/received until the command has completed. Once completed
 * the session should close the channel and optionally sent the exit status of
 * the command using {@link #sendExitStatus(int)}.</li>
 * <br>
 * <li>5. At anytime throughout the open session the client may send a signal.
 * See the SSH Connection Protocol specification for more information on this
 * advanced topic.</li>
 * </ul>
 * </p>
 * 
 * 
 */
public abstract class SessionChannelNG extends ChannelNG<SshServerContext> implements
		IdleStateListener, SessionChannelServer {

	public static final int SSH_EXTENDED_DATA_STDERR = 1;

	Subsystem subsystem;
	ExecutableCommand command;
	Map<String, String> environment = new ConcurrentHashMap<String, String>(8, 0.9f, 1);
	boolean hasTimedOut = false;
	boolean haltIncomingData = false;
	long lastActivity = System.currentTimeMillis();
	boolean agentForwardingRequested;
	boolean rawMode = false;
	
	ChannelOutputStream stderrOutputStream = new ChannelOutputStream(this, SSH_EXTENDED_DATA_STDERR);
	
	public SessionChannelNG(SshConnection con) {
		super("session", con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(), 
				0,
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize());
	}

	public void enableRawMode() {
		rawMode = true;
	}
	
	public void disableRawMode() {
		rawMode = false;
	}
	
	public Subsystem getSubsystem() {
		return subsystem;
	}
	
	final protected byte[] createChannel() throws java.io.IOException {
		registerExtendedDataType(SSH_EXTENDED_DATA_STDERR);
		return null;
	}
	
	public OutputStream getErrorStream() {
		return stderrOutputStream;
	}
	
	public boolean isAgentForwardingRequested() {
		return agentForwardingRequested;
	}
	
	/**
	 * Implement this method to support agent forwarding.
	 * 
	 * @return
	 */
	protected boolean requestAgentForwarding(String requestType) {
		return false;
	}
	
	/**
	 * If the client requests a pseudo terminal for the session this method will
	 * be invoked before the shell, exec or subsystem is started.
	 * 
	 * @param term
	 * @param cols
	 * @param rows
	 * @param width
	 * @param height
	 * @param modes
	 * @return boolean
	 */
	protected abstract boolean allocatePseudoTerminal(String term, int cols,
			int rows, int width, int height, byte[] modes);

	/**
	 * When the window (terminal) size changes on the client side, it MAY send
	 * notification in which case this method will be invoked to notify the
	 * session that a change has occurred.
	 * 
	 * @param cols
	 * @param rows
	 * @param width
	 * @param height
	 */
	protected abstract void changeWindowDimensions(int cols, int rows,
			int width, int height);

	/**
	 * A signal can be delivered to the process by the client. If a signal is
	 * received this method will be invoked so that the session may evaluate and
	 * take the required action.
	 * 
	 * @param signal
	 */
	protected abstract void processSignal(String signal);

	/**
	 * If the client requests that an environment variable be set this method
	 * will be invoked.
	 * 
	 * @param name
	 * @param value
	 * @return <tt>true</tt> if the variable has been set, otherwise
	 *         <tt>false</tt>
	 */
	public abstract boolean setEnvironmentVariable(String name, String value);

	/**
	 * Invoked when the user wants to start a shell.
	 * 
	 * @return <tt>true</tt> if the shell has been started, otherwise
	 *         <tt>false</tt>
	 */
	protected abstract boolean startShell();

	/**
	 * Invoked when the user wants to execute a command
	 * 
	 * @param cmd
	 * @return <tt>true</tt> if the cmd has been executed, otherwise
	 *         <tt>false</tt>
	 */
	protected boolean executeCommand(String[] args) {
		
		boolean success = false;
		
		try {
			command = connection.getContext().getChannelFactory().executeCommand(args, environment);
			success = true;
		} catch (UnsupportedChannelException | PermissionDeniedException e) {
			if(Log.isDebugEnabled())
				Log.debug("Failed to execute command" , e);
			success = false;
		}
		
		return success;
	}

	/**
	 * Called once the channel has been opened.
	 */
	protected void onChannelOpen() {
		if (getContext().getPolicy(ShellPolicy.class).getSessionTimeout() > 0)
			getConnectionProtocol().getTransport().getSocketConnection().getIdleStates()
					.register(this);
	}

	public boolean idle() {

		if (getContext().getPolicy(ShellPolicy.class).getSessionTimeout() > 0) {
			long idleTimeSeconds = (System.currentTimeMillis() - lastActivity) / 1000;

			if (getContext().getPolicy(ShellPolicy.class).getSessionTimeout() < idleTimeSeconds) {

				if(Log.isDebugEnabled())
					Log.debug("Session has timed out!");
				hasTimedOut = true;
				close();
				return true;
			}
		} else
			return true;

		return false;
	}

	/**
	 * Process session requests and invoke the relevant abstract methods of this
	 * class to handle the requests. If you overide this method make sure that
	 * you call the super method.
	 * 
	 * @param type
	 *            String
	 * @param wantreply
	 *            boolean
	 * @param requestdata
	 *            byte[]
	 */
	protected void onChannelRequest(String type, boolean wantreply,
			byte[] requestdata) {

		boolean success = false;

		resetIdleState();

		ByteArrayReader bar = new ByteArrayReader(
				requestdata == null ? new byte[] {} : requestdata);

		try {
			if (type.equals("pty-req")) {

				String term = bar.readString();
				int cols = (int) bar.readInt();
				int rows = (int) bar.readInt();
				int width = (int) bar.readInt();
				int height = (int) bar.readInt();
				byte[] modes = bar.readBinaryString();

				success = allocatePseudoTerminal(term, cols, rows, width,
						height, modes);
				if(Log.isDebugEnabled())
					Log.debug(term + " pseudo terminal requested");
				if(Log.isDebugEnabled())
					Log.debug("Terminal dimensions are " + String.valueOf(cols)
							+ "x" + String.valueOf(rows));
			} else if (type.equals("x11-req")) {

				boolean singleConnection = bar.readBoolean();
				String protocol = bar.readString();
				String cookie = bar.readString();
				int screen = (int) bar.readInt();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				for (int i = 0; i < cookie.length(); i += 2) {
					out.write(Integer.parseInt(cookie.substring(i, i + 2), 16));
				}

				success = getContext().getForwardingManager()
						.startX11Forwarding(singleConnection, protocol,
								out.toByteArray(), screen, getConnectionProtocol());

			} else if (type.equals("env")) {
				// read 'requestdata' as a byte stream.

				String name = bar.readString();
				String value = bar.readString();
				environment.put(name, value);
				success = setEnvironmentVariable(name, value);

				if(Log.isDebugEnabled())
					Log.debug(name + "=" + value + " environment variable set");
			} else if (type.equals("shell")) {
				boolean shellSuccess = connection.getContext().getPolicy(ShellPolicy.class).checkPermission(
								getConnection(), 
								ShellPolicy.SHELL);

				if (shellSuccess) {
					success = startShell();
				} else

					success = shellSuccess;

					EventServiceImplementation.getInstance().fireEvent(
						new Event(this,
								EventCodes.EVENT_SHELL_SESSION_STARTED,
								success).addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								getConnection()));

				if(Log.isDebugEnabled())
					Log.debug("Shell " + (success ? "started" : "failed"));

			} else if (type.equals("exec")) {

				String cmd = bar.readString();

				success = connection.getContext().getPolicy(ShellPolicy.class).checkPermission(
						getConnection(), 
						ShellPolicy.EXEC, cmd);
					

				if(success) {
					success = executeCommand(Utils.splitToArgsArray(cmd));
				}

				EventServiceImplementation.getInstance().fireEvent(
						new Event(this, EventCodes.EVENT_SHELL_COMMAND,
								success).addAttribute(
								EventCodes.ATTRIBUTE_CONNECTION,
								getConnection())
								.addAttribute(EventCodes.ATTRIBUTE_COMMAND,
										cmd));

				if(Log.isDebugEnabled())
					Log.debug("Command " + cmd
							+ (success ? " started" : " failed"));

				
			} else if (type.equals("subsystem")) {

				String name = bar.readString();

				success =  connection.getContext().getPolicy(ShellPolicy.class).checkPermission(
										getConnection(), 
										ShellPolicy.SUBSYSTEM, name);
							
				if (success) {
					try {
						subsystem = connection.getContext().getChannelFactory().createSubsystem(name, this);
					} catch (UnsupportedChannelException e) {
						success = false;
						if(Log.isDebugEnabled()) {
							Log.debug(name + " is an unsupported subsystem");
						}
					} catch (PermissionDeniedException e) {
						success = false;
						if(Log.isDebugEnabled()) {
							Log.debug(name + " could not be opened. Permission denied.");
						}
					}
				}

				if("sftp".equals(name)) {
					localWindow.setMaximumWindowSpace(connection.getContext().getPolicy(FileSystemPolicy.class).getSftpMaxWindowSize()) ;
					localWindow.setMinimumWindowSpace(connection.getContext().getPolicy(FileSystemPolicy.class).getSftpMinWindowSize());
					localWindow.setMaxiumPacketSize(connection.getContext().getPolicy(FileSystemPolicy.class).getSftpMaxPacketSize());
				} 
					
				sendWindowAdjust();

			} else if (type.equals("window-change")) {

				int cols = (int) bar.readInt();
				int rows = (int) bar.readInt();
				int width = (int) bar.readInt();
				int height = (int) bar.readInt();

				changeWindowDimensions(cols, rows, width, height);

			} else if (type.equals("signal")) {

				String signal = bar.readString();

				processSignal(signal);
			} else if(type.equals("auth-agent-req")) { 
				if(Log.isDebugEnabled()) {
					Log.debug("Agent forwarding requested for auth-agent");
				}
				success = agentForwardingRequested = requestAgentForwarding("auth-agent");
			} else if(type.equals("auth-agent-req@openssh.com")) {
				if(Log.isDebugEnabled()) {
					Log.debug("Agent forwarding requested for auth-agent@openssh.com");
				}
				success = agentForwardingRequested = requestAgentForwarding("auth-agent@openssh.com");
			}
		} catch (IOException ex) {
			if(Log.isDebugEnabled())
				Log.debug("An unexpected exception occurred", ex);
		} finally {
			bar.close();
		}

		if (success && (type.equals("exec") || type.equals("shell"))) {

			// We can now send data so send a window adjust
			sendWindowAdjust();

			if (command == null) {

				resetIdleState();
				onSessionOpen();
			}

			// Moved here because it seems some clients do not like the
			// WINDOW_ADJUST message after this has been sent.
			if (wantreply) {
				sendRequestResponse(success);
			}

			// Ensure command is started AFTER response is sent back.
			if (command != null)
				command.start();

		} else {
			// Moved here because it seems some clients do not like the
			// WINDOW_ADJUST message after this has been sent.
			if (wantreply) {
				sendRequestResponse(success);
			}
		}

		// Close channel only after sending response to request
		if (!success
				&& (type.equals("exec") || type.equals("shell") || type
						.equals("subsystem"))) {
			close();
		}

	}

//	boolean checkForExecutableCommand(String cmd) {
//
//		int idx = cmd.indexOf(' ');
//		String exec;
//		if (idx > -1) {
//			exec = cmd.substring(0, idx);
//		} else
//			exec = cmd;
//
//		if (connection.getContext().containsCommand(exec)) {
//			try {
//				command = connection.getContext().getCommand(exec).newInstance();
//				command.init(this);
//				return command.createProcess(cmd, environment);
//			} catch (IllegalAccessException ex) {
//				if(Log.isDebugEnabled())
//					Log.debug("Failed to create an ExecutableCommand", ex);
//			} catch (InstantiationException ex) {
//				if(Log.isDebugEnabled())
//					Log.debug("Failed to instantiate an ExecutableCommand", ex);
//			}
//		}
//		// Check the command here and run it
//		return false;
//	}

	/**
	 * Called when the channel is confirmed as open
	 */
	protected void onChannelOpenConfirmation() {

	}

	/**
	 * The remote side has reported EOF so no more data will be received. This
	 * will force the channel to close. If this behaviour is not required you
	 * can override this method
	 */
	protected void onRemoteEOF() {
		close();
	}

	/**
	 * Free the session and its resources. If you override this method make sure
	 * that you call the super method to ensure that the resources of the
	 * abstract class are freed.
	 */
	protected void onChannelFree() {
		if (subsystem != null) {
			subsystem.free();
		}
		subsystem = null;

		command = null;

	}

	/**
	 * Called when the channel is closing. If you override this method make sure
	 * that you call the super method.
	 */
	protected void onChannelClosing() {

		if (getContext().getPolicy(ShellPolicy.class).getSessionTimeout() > 0 && !hasTimedOut)
			getConnectionProtocol().getTransport().getSocketConnection().getIdleStates()
					.remove(this);

		if (command != null) {
			if (command.getExitCode() != ExecutableCommand.STILL_ACTIVE) {
				SessionChannelHelper.sendExitStatus(this, command.getExitCode());
			} else { 
				command.kill();
			}
		}

	}

	private void resetIdleState() {
		lastActivity = System.currentTimeMillis();
		if (getContext().getPolicy(ShellPolicy.class).getSessionTimeout() > 0)
			getConnectionProtocol().getTransport().getSocketConnection().getIdleStates()
					.reset(this);
	}

	/**
	 * Called when data arrives on the channel.
	 * 
	 * @param data
	 *            byte[]
	 */
	protected final void onChannelData(ByteBuffer data) {

		resetIdleState();

		if (subsystem != null) {
			try {
				subsystem.processMessage(data);
			} catch (IOException ex) {
				if(Log.isDebugEnabled())
					Log.debug(
							"The channel failed to process a subsystem message",
							ex);
				close();
			}
		} else {
			if(rawMode) {
				super.onChannelData(data);
			} else {
				onSessionData(data);
			}
		}
	}

	protected void onSessionData(ByteBuffer data) {
		synchronized (localWindow) {
			cache.put(data);
		}		
	}
	/**
	 * Called when extended data arrives on the channel - for a session channel
	 * this would not normally be called.
	 * 
	 * @param data
	 *            byte[]
	 * @param type
	 *            int
	 */
	protected void onExtendedData(ByteBuffer data, int type) {
		resetIdleState();
		// No such thing as stderr coming from client to server?
	}

	/**
	 * Sends stdout data to the remote client.
	 * 
	 * @param data
	 * @param off
	 * @param len
	 */
	public void sendStdoutData(byte[] data, int off, int len) throws IOException {
		resetIdleState();
		sendData(data, off, len);
	}

	/**
	 * Sends stdout data to the remote client
	 * 
	 * @param data
	 */
	public void sendStdoutData(byte[] data) throws IOException {
		resetIdleState();
		sendData(data, 0, data.length);
	}

	/**
	 * Sends stderr data to the remote client.
	 * 
	 * @param data
	 * @param off
	 * @param len
	 */
	public void sendStderrData(byte[] data, int off, int len) throws IOException {
		resetIdleState();
		sendExtendedData(data, off, len, SSH_EXTENDED_DATA_STDERR);
	}

	/**
	 * Send stderr data to the remote client.
	 * 
	 * @param data
	 */
	public void sendStderrData(byte[] data) throws IOException {
		sendStderrData(data, 0, data.length);
	}

	final protected byte[] openChannel(byte[] data)
			throws WriteOperationRequest, ChannelOpenException {
		registerExtendedDataType(SSH_EXTENDED_DATA_STDERR);
		return null;
	}

	public boolean isIncomingDataHalted() {
		return haltIncomingData;
	}
	
	@Override
	public int getMaximumWindowSpace() {
		return localWindow.getMaximumWindowSpace();
	}

	@Override
	public int getMinimumWindowSpace() {
		return localWindow.getMinimumWindowSpace();
	}

	@Override
	public void onSessionOpen() {
		
	}
}
