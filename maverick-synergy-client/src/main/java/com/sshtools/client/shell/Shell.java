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
/* HEADER */
package com.sshtools.client.shell;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.tasks.AbstractSessionTask;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;

public class Shell {

	/** Windows operating system **/
	public static final int OS_WINDOWS = 1;
	/** Linux operating system **/
	public static final int OS_LINUX = 2;
	/** Solaris operating system **/
	public static final int OS_SOLARIS = 3;
	/** AIX operating system **/
	public static final int OS_AIX = 4;
	/** Darwin (MAC) operating system **/
	public static final int OS_DARWIN = 5;
	/** FreeBSD operating system **/
	public static final int OS_FREEBSD = 6;
	/** OpenBSD operating system **/
	public static final int OS_OPENBSD = 7;
	/** NetBSD operating system **/
	public static final int OS_NETBSD = 8;
	/** HP-UX operating system **/
	public static final int OS_HPUX = 9;
	
	/** Unix OS if less than this value. **/
	public static final int OS_UNIX = 20;
	
	/** OpenVMS operating system **/
	public static final int OS_OPENVMS = 21;

	/** The operating system is unknown **/
	public static final int OS_UNKNOWN = 99;

	private int osType = OS_UNKNOWN;
	private String osDescription = "Unknown";
	private String passwordErrorText = "Sorry, try again.";
	private String passwordPrompt = "Password:";
	// These are markers for the beginning and end of the command
	static final String BEGIN_COMMAND_MARKER = "---BEGIN---";
	static final String END_COMMAND_MARKER = "---END---";
	static final String PROCESS_MARKER = "PROCESS=";
	static final String EXIT_CODE_MARKER = "EXITCODE=";

	// Our states, we are either waiting to execute a command or processing.
	static final int WAITING_FOR_COMMAND = 1;
	static final int PROCESSING_COMMAND = 2;
	static final int CLOSED = 3;

	BufferedInputStream sessionIn;
	OutputStream sessionOut;
	int state = WAITING_FOR_COMMAND;
	boolean inStartup;

	// Variables that change change according to operating system
	private String PIPE_CMD = "";
	private String ECHO_COMMAND = "echo";
	private String EOL = "\r\n";
	private String EXIT_CODE_VARIABLE = "%errorlevel%";
	private static int SHELL_INIT_PERIOD = 2000;

	List<Runnable> closeHooks = new ArrayList<Runnable>();

	int numCommandsExecuted = 0;

	private static boolean verboseDebug = Boolean
			.getBoolean("maverick.shell.verbose");

	private StartupInputStream startupIn;
	private ShellController startupController;
	private boolean childShell = false;

	public static final int EXIT_CODE_PROCESS_ACTIVE = Integer.MIN_VALUE;
	public static final int EXIT_CODE_UNKNOWN = Integer.MIN_VALUE + 1;

	long startupTimeout;
	long startupStarted;
	
	AbstractSessionTask<SessionChannelNG> session;
	String characterEncoding = "UTF-8";
	
	public Shell(AbstractSessionTask<SessionChannelNG> session) throws SshException, SshIOException,
			ChannelOpenException, IOException, ShellTimeoutException {
		this(session, null, 30000, "dumb", 1024, 80);
	}

	public Shell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger)
			throws SshException, SshIOException, ChannelOpenException,
			IOException, ShellTimeoutException {
		this(session, trigger, 30000, "dumb", 1024, 80);
	}

	public Shell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger, long startupTimeout)
			throws SshException, SshIOException, ChannelOpenException,
			IOException, ShellTimeoutException {
		this(session, trigger, startupTimeout, "dumb", 1024, 80);
	}

	public Shell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger,
			long startupTimeout, String termtype) throws SshException,
			SshIOException, ChannelOpenException, IOException,
			ShellTimeoutException {
		this(session, trigger, startupTimeout, termtype, 1024, 80);
	}

	public Shell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger,
			long startupTimeout, String termtype, int cols, int rows)
			throws SshException, SshIOException, ChannelOpenException,
			IOException, ShellTimeoutException {

		this.startupTimeout = startupTimeout;
		this.startupStarted = System.currentTimeMillis();
		this.session = session; 
		
		if(Log.isDebugEnabled())
			Log.debug("Creating session for interactive shell");

		closeHooks.add(new Runnable() {
			public void run() {
				Shell.this.session.close();
			}
		});
		// Allow the shell to initialize before we start sending data
		if(SHELL_INIT_PERIOD > 0) {
			try {
				Thread.sleep(SHELL_INIT_PERIOD);
			} catch (InterruptedException e) {
			}
		}
		
		determineServerType(session.getSession().getConnection());

		init(session.getSession().getInputStream(), session.getSession().getOutputStream(), // true, trigger);
		        (osType != OS_OPENVMS), trigger );
	}

	
	Shell(InputStream in, OutputStream out, String eol, String echoCmd,
			String exitCodeVar, int osType, String osDescription, Shell parentShell)
			throws SshIOException, SshException, IOException,
			ShellTimeoutException {
		this.EOL = eol;
		this.ECHO_COMMAND = echoCmd;
		this.EXIT_CODE_VARIABLE = exitCodeVar;
		this.osType = osType;
		this.osDescription = osDescription;
		this.childShell = true;
		init(in, out, true, null);
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}


	public static void setShellInitTimeout(int timeout) {
		SHELL_INIT_PERIOD = timeout;
	}
	
	public InputStream getStartupInputStream() {
		return startupIn;
	}
	
	void determineServerType(Connection<SshClientContext> con )
	{
	    String remoteID = con.getRemoteIdentification();
	    
	    if ( remoteID.indexOf( "OpenVMS" ) > 0 )
	    {
	        osType = OS_OPENVMS;
	        PIPE_CMD = "PIPE ";
	        ECHO_COMMAND = "WRITE SYS$OUTPUT";
	        EXIT_CODE_VARIABLE = "$SEVERITY";
	    }
	}

	void init(InputStream in, OutputStream out, boolean detectSettings,
			ShellStartupTrigger trigger) throws SshIOException, SshException,
			IOException, ShellTimeoutException {

		sessionIn = new BufferedInputStream(in);
		sessionOut = out;

		startupIn = new StartupInputStream(BEGIN_COMMAND_MARKER,
				detectSettings, trigger);

		if(Log.isDebugEnabled())
			Log.debug("Session creation complete");

	}

	public boolean inStartup() {
		return inStartup;
	}
	
	public void setPasswordErrorText(String passwordErrorText) {
		this.passwordErrorText = passwordErrorText;
	}
	
	public void setPasswordPrompt(String passwordPrompt) {
		this.passwordPrompt = passwordPrompt;
	}
	
	public ShellReader getStartupReader() {
		return startupController;
	}

	public Shell su(String cmd, String password) throws SshIOException,
			SshException, IOException, ShellTimeoutException {
		return su(cmd, password, passwordPrompt, new ShellDefaultMatcher());
	}

	public Shell su(String cmd, String password, String promptExpression)
			throws SshException, SshIOException, IOException,
			ShellTimeoutException {
		return su(cmd, password, promptExpression, new ShellDefaultMatcher());
	}

	public Shell su(String cmd) throws SshException, SshIOException,
			IOException, ShellTimeoutException {
		ShellProcess process = executeCommand(cmd, false, false);
		return new Shell(process.getInputStream(), process.getOutputStream(),
				EOL, ECHO_COMMAND, EXIT_CODE_VARIABLE, osType, osDescription, this);
	}

	public Shell su(String cmd, String password, String promptExpression,
			ShellMatcher matcher) throws SshException, SshIOException,
			IOException, ShellTimeoutException {
		ShellProcess process = executeCommand(cmd, false, false);
		ShellProcessController contr = new ShellProcessController(process,
				matcher);
		process.mark(1024);
		if (contr.expectNextLine(promptExpression)) {
			if(Log.isDebugEnabled())
				Log.debug("su password expression matched");
			contr.typeAndReturn(password);
			contr.readLine();
			process.mark(1024);
			if(contr.expectNextLine(passwordErrorText)) {
				throw new IOException("Incorrect password!");
			}
			process.reset();
		} else {
			if(Log.isDebugEnabled())
				Log.debug("su password expression not matched");
			process.reset();
		}

		if (process.isActive()) {
			return new Shell(process.getInputStream(),
					process.getOutputStream(), EOL, ECHO_COMMAND,
					EXIT_CODE_VARIABLE, osType, osDescription, this);
		} else {
			throw new SshException("The command failed: " + cmd,
					SshException.SHELL_ERROR);
		}
	}

	public ShellProcess sudo(String cmd, String password) throws SshException,
			ShellTimeoutException, IOException {
		return sudo(cmd, password, passwordPrompt, new ShellDefaultMatcher());
	}

	public ShellProcess sudo(String cmd, String password,
			String promptExpression) throws SshException,
			ShellTimeoutException, IOException {
		return sudo(cmd, password, promptExpression, new ShellDefaultMatcher());
	}

	public ShellProcess sudo(String cmd, String password,
			String promptExpression, ShellMatcher matcher) throws SshException,
			ShellTimeoutException, IOException {
		ShellProcess process = executeCommand(cmd, false, false);
		ShellProcessController contr = new ShellProcessController(process,
				matcher);
		process.mark(1024);
		if (contr.expectNextLine(promptExpression)) {
			if(Log.isDebugEnabled())
				Log.debug("sudo password expression matched");
			contr.typeAndReturn(password);
			process.mark(1024);
			if(contr.expectNextLine(passwordErrorText)) {
				throw new IOException("Incorrect password!");
			}
			process.reset();
		} else {
			if(Log.isDebugEnabled())
				Log.debug("sudo password expression not matched");
			process.reset();
		}
		return process;
	}

	public boolean isClosed() {
		return state == CLOSED;
	}

	private void updateDescription() {

		if (osType == OS_SOLARIS) {
			osDescription = "Solaris";
		} else if (osType == OS_AIX) {
			osDescription = "AIX";
		} else if (osType == OS_WINDOWS) {
			osDescription = "Windows";
		} else if (osType == OS_DARWIN) {
			osDescription = "Darwin";
		} else if (osType == OS_FREEBSD) {
			osDescription = "FreeBSD";
		} else if (osType == OS_OPENBSD) {
			osDescription = "OpenBSD";
		} else if (osType == OS_NETBSD) {
			osDescription = "NetBSD";
		} else if (osType == OS_LINUX) {
			osDescription = "Linux";
		} else if (osType == OS_HPUX) {
			osDescription = "HP-UX";
		} else if (osType == OS_OPENVMS) {
		    osDescription = "OpenVMS";
		} else {
			osDescription = "Unknown";
		}
	}

	public void exit() throws IOException, SshException {
		sessionOut.write(("exit" + EOL).getBytes());
		if (childShell) {
			while (sessionIn.read() > -1)
				;
		}
		close();
	}

	public void close() throws IOException, SshException {
		internalClose();
	}

	public String getNewline() {
		if (osType == OS_WINDOWS) {
			return "\r\n";
		} else {
			return "\n";
		}
	}

	public synchronized ShellProcess executeCommand(String origCmd)
			throws SshException {
		return executeCommand(origCmd, false, false, "UTF-8");
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			boolean consume) throws SshException {
		return executeCommand(origCmd, false, consume, "UTF-8");
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			String charset) throws SshException {
		return executeCommand(origCmd, false, false, charset);
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			boolean consume, String charset) throws SshException {
		return executeCommand(origCmd, false, consume, charset);
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			boolean matchPromptMarker, boolean consume) throws SshException {
		return executeCommand(origCmd, matchPromptMarker, consume, "UTF-8");
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			boolean matchPromptMarker, boolean consume, String charset)
			throws SshException {

		try {
			
			String cmd = origCmd;

			if (state == PROCESSING_COMMAND)
				throw new SshException("Command still active",
						SshException.BAD_API_USAGE);
			if (state == CLOSED)
				throw new SshException("Shell is closed!",
						SshException.BAD_API_USAGE);

			checkStartupFinished();

			state = PROCESSING_COMMAND;

			StringBuffer prompt = new StringBuffer();

			// Override matchPromptMarker if using . on HP-UX
			matchPromptMarker = matchPromptMarker
					| ((origCmd.startsWith(".") || origCmd.startsWith("source")) && osType == OS_HPUX);

			if (matchPromptMarker) {

				// Get the prompt value
				sessionOut.write(EOL.getBytes());
				sessionOut.write(EOL.getBytes());

				int ch;
				while ((ch = sessionIn.read()) > -1 && ch != '\n')
					;

				while ((ch = sessionIn.read()) > -1 && ch != '\n')
					prompt.append((char) ch);

				if(Log.isDebugEnabled())
					Log.debug("Prompt is " + prompt.toString().trim());

			}

			if(Log.isDebugEnabled())
				Log.debug("Executing command: " + cmd);

			/**
			 * Create the command
			 * 
			 * We echo out a marker to identify the beginning of the process
			 * output, this also allows us to consume any unwanted prompt output
			 * that may be in the session output. We then execute the command
			 * followed by a further echo out of the end marker. We need to
			 * ensure we get the end marker for both a successful process exit
			 * and an unsuccessful exit so we duplicate end marker echo with &&
			 * and ||. This seems to be supported in both *nix and Windows so
			 * should work in most scenarios.
			 */
			String echoCmd;

			String endCommand = nextEndMarker();

			if (osType == OS_WINDOWS) {
				// %errorlevel% doesnt work properly on multiple command line so
				// we fix it to 0 and 1 for good or bad result
			    echoCmd = ECHO_COMMAND + " " + BEGIN_COMMAND_MARKER + " && " + cmd
		                		+ " && " + ECHO_COMMAND + " " + endCommand + "0"
						+ " || " + ECHO_COMMAND + " " + endCommand + "1" + EOL;
			} else if ( osType == OS_OPENVMS ) {
			    // Do same trick with end marker for OpenVMS via its PIPE command.
				echoCmd = PIPE_CMD + ECHO_COMMAND + " \"" + BEGIN_COMMAND_MARKER + "\" && " + cmd
		                		+ " && " + ECHO_COMMAND + " \"" + endCommand + "0\" || "
		                		+ ECHO_COMMAND + "\"" + endCommand + "1\"" + EOL;
			} else {
			    // Assume it's a Unix system and 'echo' works.
                echoCmd = "echo \"" + BEGIN_COMMAND_MARKER + "\"; " + cmd
                        + "; echo \"" + endCommand + EXIT_CODE_VARIABLE + "\"" + EOL;
			}

			sessionOut.write(echoCmd.getBytes(charset));

			numCommandsExecuted++;

			ShellInputStream in  = new ShellInputStream(
					this, BEGIN_COMMAND_MARKER, endCommand, origCmd,
					matchPromptMarker, prompt.toString().trim());
			ShellProcess process = new ShellProcess(this, in);

			if (consume) {
				while (process.getInputStream().read() > -1) {

				}
			}
			return process;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException("Failed to execute command: "
					+ ex.getMessage(), SshException.CHANNEL_FAILURE);
		}
	}

	public int getNumCommandsExecuted() {
		return numCommandsExecuted;
	}

	private void checkStartupFinished() throws IOException {

		if(Log.isDebugEnabled())
			Log.debug("Checking state of startup controller");

		if (!startupIn.isClosed()) {
			if(Log.isDebugEnabled())
				Log.debug("Shell still in startup mode, draining startup output");
			while (startupIn.read() > -1)
				;
		}

		if(Log.isDebugEnabled())
			Log.debug("Shell is ready for command");
	}

	private synchronized String nextEndMarker() {
		return END_COMMAND_MARKER + ";" + PROCESS_MARKER
				+ System.currentTimeMillis() + ";" + EXIT_CODE_MARKER;
	}

	public int getOsType() {
		return osType;
	}

	public String getOsDescription() {
		return osDescription;
	}

	/**
	 * Type some characters as input on the remote shell.
	 * 
	 * @param string
	 *            String
	 * @throws IOException
	 */
	void type(String string) throws IOException {
		write(string.getBytes());
	}

	/**
	 * Write some data as input on the remote shell.
	 * 
	 * @param bytes
	 * @throws IOException
	 */
	void write(byte[] bytes) throws IOException {
		sessionOut.write(bytes);
	}

	/**
	 * Write a byte of data as input on the remote shell.
	 * 
	 * @param b
	 * @throws IOException
	 */
	void type(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	/**
	 * Send a carriage return to the remote shell.
	 * 
	 * @throws IOException
	 */
	void carriageReturn() throws IOException {
		write(EOL.getBytes());
	}

	/**
	 * Type some characters followed by a carriage return on the remote shell.
	 * 
	 * @param string
	 *            String
	 * @throws IOException
	 */
	void typeAndReturn(String string) throws IOException {
		write((string + EOL).getBytes());
	}

	void internalClose() {
		state = CLOSED;

		for (Runnable r : closeHooks) {
			try {
				r.run();
			} catch (Throwable t) {
			}
		}
	}

	class StartupInputStream extends InputStream {

		char[] marker1;
		int markerPos;
		StringBuffer currentLine = new StringBuffer();
		boolean detectSettings;

		StartupInputStream(String marker1str, boolean detectSettings,
				ShellStartupTrigger trigger) throws SshException, IOException,
				ShellTimeoutException {
			this.detectSettings = detectSettings;
			this.marker1 = marker1str.toCharArray();

			startupController = new ShellController(Shell.this,
					new ShellDefaultMatcher(), this);

			// As we are attempting to detect settings we don't use an END
			// marker
			if (trigger != null) {
				StringBuffer line = new StringBuffer();
				int ch;
				do {
					ch = internalRead(sessionIn);
					if (ch != '\n' && ch != '\r' && ch != -1)
						line.append((char) ch);
					if (ch == '\n')
						line.setLength(0);
					if (ch == -1)
						throw new SshException(
								"Shell output ended before trigger could start shell",
								SshException.PROMPT_TIMEOUT);

				} while (!trigger.canStartShell(line.toString(),
						startupController));
			}

			if (detectSettings) {

			    String cmd = PIPE_CMD + ECHO_COMMAND + " \"" + marker1str + "\" ; " + ECHO_COMMAND + " $?" + "\r\n";

				if(Log.isDebugEnabled())
					Log.debug("Performing marker test: " + cmd);

				sessionOut.write(cmd.getBytes());
			}

			inStartup = detectSettings;

		}

		boolean isClosed() {
			return !inStartup;
		}

		int internalRead(InputStream in) throws IOException {

			do {
				try {
					return in.read();
				} catch (SshIOException e) {
					if (e.getRealException().getReason() == SshException.MESSAGE_TIMEOUT) {
						if (System.currentTimeMillis() - startupStarted > startupTimeout)
							throw new SshIOException(new SshException("",
									SshException.PROMPT_TIMEOUT));
					} else
						throw e;
				}
			} while (true);

		}

		String internalReadLine(InputStream in) throws IOException {

			StringBuffer tmp = new StringBuffer();
			int ch;

			do {
				ch = internalRead(in);
				if (ch > -1)
					tmp.append((char) ch);
			} while (ch != -1 && ch != '\n');

			return tmp.toString().trim();
		}

		@Override
		public int read() throws IOException {

			int ch;
			if (inStartup) {

				sessionIn.mark(marker1.length + 1);
				StringBuffer tmp = new StringBuffer();

				while (true) {
					try {
						do {

							ch = internalRead(sessionIn);
							tmp.append((char) ch);
						} while (markerPos < marker1.length - 1
								&& marker1[markerPos++] == ch);

						break;
					} catch (SshIOException e) {
						if (e.getRealException().getReason() == SshException.MESSAGE_TIMEOUT) {
							if (System.currentTimeMillis() - startupStarted > startupTimeout)
								throw new SshIOException(new SshException("",
										SshException.PROMPT_TIMEOUT));
						} else
							throw e;
					}
				}
				if (markerPos == marker1.length - 1) {

					if(Log.isDebugEnabled())
						Log.debug("Potentially found test marker ["
								+ currentLine.toString() + tmp.toString() + "]");

					// Is this just erroneous echo?
					ch = internalRead(sessionIn);

					if (ch == '\r') {
						if(Log.isDebugEnabled())
							Log.debug("Looking good, found CR");
						ch = internalRead(sessionIn);
					}
					if (ch == '\n') {
						// We matched the marker!!!
						if(Log.isDebugEnabled())
							Log.debug("Found test marker");

						try {
							detect();
						} catch (SshException e) {
							throw new SshIOException(e);
						}
						return -1;
					} else {
						if(Log.isDebugEnabled())
							Log.debug("Detected echo of test marker command since we did not find LF at end of marker ch="
									+ Integer.valueOf(ch)
									+ " currentLine="
									+ currentLine.toString() + tmp.toString());
					}
				}

				sessionIn.reset();
				ch = internalRead(sessionIn);

				markerPos = 0;
				currentLine.append((char) ch);

				if (ch == '\n') {
					if(Log.isDebugEnabled())
						Log.debug("Shell startup (read): "
								+ currentLine.toString());
					// End of a line
					currentLine = new StringBuffer();
				}

				if (verboseDebug && Log.isDebugEnabled())
					Log.debug("Shell startup (read): " + currentLine.toString());

				sessionIn.mark(-1);

				return ch;
			}

			return -1;
		}

		void detect() throws IOException, SshException {

			inStartup = false;

			if (!detectSettings)
				return;

			if(Log.isDebugEnabled())
				Log.debug("Detecting shell settings");

			// This should be the value output by our echo command
			String line = internalReadLine(sessionIn);

			if(Log.isDebugEnabled())
				Log.debug("Shell startup (detect): " + line);

			// Validate the output, if it has been processed correctly then it
			// should be a *nix type shell
			if (line.equals("0") && osType == OS_UNKNOWN ) {
			    if(Log.isDebugEnabled())
					Log.debug("This looks like a *nix type machine, setting EOL to CR only and exit code variable to $?");
				EOL = "\r";
				EXIT_CODE_VARIABLE = "$?";

				// Attempt to execute uname for some information
				ShellProcess proc = executeCommand("uname");
				BufferedReader r2 = new BufferedReader(new InputStreamReader(
						proc.getInputStream()));

				String tmp;
				line = "";
				while ((tmp = r2.readLine()) != null)
					line += tmp;

				switch (proc.getExitCode()) {
				case 0:
					if(Log.isDebugEnabled())
						Log.debug("Remote side reported it is " + line.trim());

					line = line.toLowerCase();
					if (line.startsWith("Sun")) {
						osType = OS_SOLARIS;
					} else if (line.startsWith("aix")) {
						osType = OS_AIX;
					} else if (line.startsWith("darwin")) {
						osType = OS_DARWIN;
					} else if (line.startsWith("freebsd")) {
						osType = OS_FREEBSD;
					} else if (line.startsWith("openbsd")) {
						osType = OS_OPENBSD;
					} else if (line.startsWith("netbsd")) {
						osType = OS_NETBSD;
					} else if (line.startsWith("linux")) {
						osType = OS_LINUX;
					} else if (line.startsWith("hp-ux")) {
						osType = OS_HPUX;
					} else {
						osType = OS_UNKNOWN;
					}
					break;
				case 127:
					Log.debug("Remote side does not support uname");
					break;
				default:
					Log.debug("uname returned error code " + proc.getExitCode());
				}

			} else if (osType == OS_UNKNOWN) {
				String cmd = "echo " + BEGIN_COMMAND_MARKER
						+ " && echo %errorlevel%\r\n";
				sessionOut.write(cmd.getBytes());
				while ((line = internalReadLine(sessionIn)) != null
						&& !line.endsWith(BEGIN_COMMAND_MARKER)) {
					if (!line.trim().equals("")) {
						if(Log.isDebugEnabled())
							Log.debug("Shell startup: " + line);
					}
				}

				line = internalReadLine(sessionIn);

				if (line.equals("0")) {
					if(Log.isDebugEnabled())
						Log.debug("This looks like a Windows machine, setting EOL to CRLF and exit code variable to %errorlevel%");
					EOL = "\r\n";
					EXIT_CODE_VARIABLE = "%errorlevel%";
					osType = OS_WINDOWS;
				}
			}

			updateDescription();

			if(Log.isDebugEnabled())
				Log.debug("Setting default sudo prompt");
			
			executeCommand("export SUDO_PROMPT=Password:", true);
			
			if(Log.isDebugEnabled())
				Log.debug("Shell initialized");
		}

	}

}
