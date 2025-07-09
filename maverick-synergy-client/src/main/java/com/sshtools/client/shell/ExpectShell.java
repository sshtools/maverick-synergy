package com.sshtools.client.shell;

/*-
 * #%L
 * Client API
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.tasks.AbstractSessionTask;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;

/**
 * Execute commands within a shell, capturing the output of just the command
 * itself. 
 */
public class ExpectShell {
	
	private static final String PASSWORD_ERROR_TEXT = "Sorry, try again.";
	private static final String DEFAULT_PASSWORD_PROMPT = "Password:";

	/**
	 * Enumeration of operating systems, that also encapsulates 
	 * some operating specific information
	 */
	public enum OS {
		WINDOWS, LINUX, SOLARIS, AIX, DARWIN, FREEBSD, OPENBSD, NETBSD, HPUX, UNIX, OPENVMS, POWERSHELL, UNKNOWN;
		
		/**
		 * Get the legacy operating system code for this operating system.
		 * 
		 * @return code 
		 */
		@Deprecated(forRemoval = true, since = "3.1.3")
		public int code() {
			switch(this) {
			case WINDOWS:
				return 1;
			case LINUX:
				return 2;
			case SOLARIS:
				return 3;
			case AIX:
				return 4;
			case DARWIN:
				return 5;
			case FREEBSD:
				return 6;
			case OPENBSD:
				return 7;
			case NETBSD:
				return 8;
			case HPUX:
				return 9;
			case UNIX:
				return 20;
			case OPENVMS:
				return 21;
			case POWERSHELL:
				return 22;
			default:
				return 99;
			}
		}
		
		/**
		 * Get an {@link OS} given the legacy code (see the constants in {@link ExpectShell}.
		 * @param code legacy code
		 * @return operating system constant
		 */
		@Deprecated(forRemoval = true, since = "3.1.3")
		public static OS code(int code) {
			switch(code) {
			case 1:
				return WINDOWS;
			case 2:
				return LINUX;
			case 3:
				return SOLARIS;
			case 4:
				return AIX;
			case 5:
				return DARWIN;
			case 6:
				return FREEBSD;
			case 7:
				return OPENBSD;
			case 8:
				return NETBSD;
			case 9:
				return HPUX;
			case 20:
				return UNIX;
			case 21:
				return OPENVMS;
			case 22:
				return POWERSHELL;
			default:
				return UNKNOWN;
			}
		}
		
		/**
		 * Get a description of the operating system.
		 * 
		 * @return description
		 */
		public String description() {
			switch(this) {
			case WINDOWS:
				return "Windows";
			case LINUX:
				return "Linux";
			case SOLARIS:
				return "Solaris";
			case AIX:
				return "AIX";
			case DARWIN:
				return "Darwin";
			case FREEBSD:
				return "FreeBSD";
			case OPENBSD:
				return "OpenBSD";
			case NETBSD:
				return "NetBSD";
			case HPUX:
				return "HP-UX";
			case UNIX:
				return "UNIX";
			case OPENVMS:
				return "OpenVMS";
			case POWERSHELL:
				return "Windows PowerShell";
			default:
				return "Unknown";
			}
		}

		/**
		 * Get the exit code variable this operating system uses.
		 * 
		 * @return exit code variable
		 */
		public String exitCodeVariable() {
			switch(this) {
			case WINDOWS:
				return "%errorlevel%";
			case OPENVMS:
		        return "$SEVERITY";
			default:
				return "$?";
			}
		}
		
		/**
		 * Get the command this operating system uses to echo content
		 * back to the terminal.
		 * 
		 * @return echo command
		 */
		public String echoCommand() {
			switch(this) {
			case WINDOWS:
				return "\r\n";
			case OPENVMS:
				return "WRITE SYS$OUTPUT";
			default:
				return "echo";
			}
		}
		
		/**
		 * Get the pipe command, if this OS has one.
		 * 
		 * @return pipe command
		 */
		public String pipeCommand() {
			switch(this) {
			case OPENVMS:
				return "PIPE";
			default:
				return "";
			}
		}
		
		/**
		 * Get the end of line sequence.
		 * 
		 * @return
		 */
		public String eol() {
			switch(this) {
			case WINDOWS:
				return "\r\n";
			default:
				return "\r";
			}
		}
	}
	
	/**
	 * Builder that creates an {@link ExpectShell}.
	 * 
	 */
	public final static class ExpectShellBuilder {
		private Optional<OS> os = Optional.empty();
		private Duration startupTimeout = Duration.ofSeconds(30);
		private Optional<InputStream> input = Optional.empty();
		private Optional<OutputStream> output = Optional.empty();
		private Optional<SessionChannelNG> session = Optional.empty();
		private Optional<String> remoteIdentification = Optional.empty();
		private Optional<Charset> encoding = Optional.empty();
		private Optional<String> passwordPrompt = Optional.empty();
		private Optional<String> passwordErrorText = Optional.empty();
		private Optional<ShellStartupTrigger> trigger = Optional.empty();
		private boolean detectSettings = true;
		
		/**
		 * Create a new builder.
		 * 
		 * @return builder
		 */
		public static ExpectShellBuilder create() {
			return new ExpectShellBuilder();
		}
		
		/**
		 * Prevent operating system and it's settings being  
		 * detected by examining the stream.
		 * 
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withoutDetectSettings() {
			return withDetectSettings(false);
		}
		
		/**
		 * Set whether the operating system and it's settings should be 
		 * detected by examining the stream.
		 * 
		 * @param detectSettings detect settings
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withDetectSettings(boolean detectSettings) {
			this.detectSettings = detectSettings;
			return this;
		}
	
		/** 
		 * Set a callback that is invoked to query if a command line would allow a shell
		 * to start.
		 * 
		 * @param trigger shell startup trigger
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withTrigger(ShellStartupTrigger trigger) {
			this.trigger = Optional.of(trigger);
			return this;
		}
	
		/** 
		 * Set the pattern to look for to indicate failure of a password when
		 * prompted. Defaults to {@link ExpectShell#PASSWORD_ERROR_TEXT}. 
		 * This may be a regular expression.
		 * 
		 * @param passwordErrorText password error text
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withPasswordErrorText(String passwordErrorText) {
			this.passwordErrorText = Optional.of(passwordErrorText);
			return this;
		}
	
		/** 
		 * Set the pattern to look for for elevated commands using {@link ExpectShell#su(String)} and friends
		 * that require a password. Defaults to {@link ExpectShell#DEFAULT_PASSWORD_PROMPT}. This may be
		 * a regular expression.
		 * 
		 * @param password prompt
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withPasswordPrompt(String passwordPrompt) {
			this.passwordPrompt = Optional.of(passwordPrompt);
			return this;
		}
	
		/** 
		 * Set the character encoding to use for transferring string content.
		 * 
		 * @param encoding encoding 
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withEncoding(String encoding) {
			if(encoding == null) {
				this.encoding = Optional.empty();
				return this;
			}
			return withEncoding(Charset.forName(encoding));
		}
		
		/** 
		 * Set the character encoding to use for transferring string content.
		 * 
		 * @param encoding encoding 
		 * @return builder for chaining
		 */
		public ExpectShellBuilder withEncoding(Charset encoding) {
			this.encoding = Optional.of(encoding);
			return this;
		} 
		
		/**
		 * Select the {@link InputStream} to read input from. You would either 
		 * manually set the {@link InputStream} and {@link OutputStream}, or alternatively
		 * provide a {@link SessionChannelNG} from which to derive the streams.
		 */
		public ExpectShellBuilder withInput(InputStream input) {
			this.input = Optional.of(input);
			return this;
		}
		
		/**
		 * Select the {@link OutputStream} to write output to. You would either 
		 * manually set the {@link InputStream} and {@link OutputStream}, or alternatively
		 * provide a {@link SessionChannelNG} from which to derive the streams.
		 */
		public ExpectShellBuilder withOutput(OutputStream output) {
			this.output = Optional.of(output);
			return this;
		}
		
		/**
		 * Specify the session to use, from which the {@link InputStream} and
		 * {@link OutputStream} can be derived. You may alternatively specify
		 * the streams yourself.
		 * <p>
		 * Providing the session will also allow the remote server identification
		 * to be automatically queries.
		 * 
		 * @param session session
		 * @return this for chaining 
		 */
		public ExpectShellBuilder withSession(SessionChannelNG session) {
			this.session = Optional.of(session);
			return this;
		}
		
		/**
		 * Specify the task from which to derive the session to use, 
		 * from which the {@link InputStream} and
		 * {@link OutputStream} can be derived. You may alternatively specify
		 * the streams yourself.
		 * 
		 * @param task task
		 * @return this for chaining
		 */
		public ExpectShellBuilder withTask(AbstractSessionTask<SessionChannelNG> task) {
			return withSession(task.getSession());
		}
		
		/**
		 * Specify the remote server identification, which may be used as part 
		 * of operating system detection. When not provided, and either a {@link SessionChannelNG}
		 * or a {@link AbstractSessionTask} has been provided, the remote identification can be
		 * obtain automatically.
		 * 
		 * @param remoteIdentification remote identification
		 * @return this for chaining
		 */
		public ExpectShellBuilder withSession(AbstractSessionTask<SessionChannelNG> session) {
			return withSession(session.getSession());
		}
		
		/**
		 * Specify the operating system for this shell. This will determine various
		 * parameters such as the commands to run, exit code variables, newline sequences
		 * and more.
		 * <p>
		 * When not specified, the default will be determined by examining the output and
		 * environment.
		 * 
		 * @param os operating system
		 * @return this for chaining
		 */
		public ExpectShellBuilder withOS(OS os) {
			this.os = Optional.of(os);
			return this;
		}

		/**
		 * Specify the operating system for this shell. This will determine various
		 * parameters such as the commands to run, exit code variables, newline sequences
		 * and more.
		 * <p>
		 * When not specified, the default will be determined by examining the output and
		 * environment.
		 * 
		 * @param os operating system legacy code
		 * @return this for chaining
		 */
		@Deprecated(forRemoval = true, since = "3.1.3")
		public ExpectShellBuilder withOS(int os) {
			return withOS(OS.code(os));
		}
		
		/**
		 * How long to wait for the shells first prompt before giving up and failing.
		 *  
		 * @param startupTimeout startup timeout
		 * @return this for chaining
		 */
		public ExpectShellBuilder withStartupTimeout(Duration startupTimeout) {
			this.startupTimeout  = startupTimeout;
			return this;
		}
		
		/**
		 * How long to wait in seconds for the shells first prompt before giving up and failing.
		 *  
		 * @param startupTimeout startup timeout in seconds
		 * @return this for chaining
		 */
		public ExpectShellBuilder withStartupTimeoutSec(int startupTimeout) {
			return withStartupTimeout(Duration.ofSeconds(startupTimeout));
		}
		
		/**
		 * Build the shell.
		 * 
		 * @return shell
		 * @throws SshException on SSH error
		 * @throws IOException on IO error
		 * @throws ShellTimeoutException on timeout
		 */
		public ExpectShell build() throws SshException, IOException, ShellTimeoutException {
			return new ExpectShell(this);
		}
	}

	/** Windows operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_WINDOWS = 1;
	/** Linux operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_LINUX = 2;
	/** Solaris operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_SOLARIS = 3;
	/** AIX operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_AIX = 4;
	/** Darwin (MAC) operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_DARWIN = 5;
	/** FreeBSD operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_FREEBSD = 6;
	/** OpenBSD operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_OPENBSD = 7;
	/** NetBSD operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_NETBSD = 8;
	/** HP-UX operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_HPUX = 9;
	
	/** Unix OS if less than this value. **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_UNIX = 20;
	
	/** OpenVMS operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_OPENVMS = 21;

	/** Linux operating system **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_POWERSHELL = 22;
	
	/** The operating system is unknown **/
	@Deprecated(forRemoval = true, since = "3.1.3")
	public static final int OS_UNKNOWN = 99;

	private final OS osType;
	
	// These are markers for the beginning and end of the command
	static final String BEGIN_COMMAND_MARKER = "---BEGIN---";
	static final String END_COMMAND_MARKER = "---END---";
	static final String PROCESS_MARKER = "PROCESS=";
	static final String EXIT_CODE_MARKER = "EXITCODE=";

	// Our states, we are either waiting to execute a command or processing.
	static final int WAITING_FOR_COMMAND = 1;
	static final int PROCESSING_COMMAND = 2;
	static final int CLOSED = 3;
	
	int state = WAITING_FOR_COMMAND;
	
	@Deprecated
	private static int SHELL_INIT_PERIOD = 2000;

	List<Runnable> closeHooks = new ArrayList<Runnable>();
	Optional<String> sudoPassword = Optional.empty();
	
	int numCommandsExecuted = 0;

	private final static boolean verboseDebug = Boolean.getBoolean("maverick.shell.verbose");

	private final StartupInputStream startupIn;
	private final boolean childShell;

	public static final int EXIT_CODE_PROCESS_ACTIVE = Integer.MIN_VALUE;
	public static final int EXIT_CODE_UNKNOWN = Integer.MIN_VALUE + 1;

	private final Duration startupTimeout;
	
	/* TODO: Will be made final at 3.3.0, and associated setters removed */
	private String passwordErrorText;
	private String passwordPrompt;
	private Charset characterEncoding;

	@Deprecated(since = "3.2.0", forRemoval = true)
	public ExpectShell(AbstractSessionTask<SessionChannelNG> session) throws SshException, IOException, ShellTimeoutException {
		this(session, null, 30000);
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public ExpectShell(AbstractSessionTask<SessionChannelNG> session, int osType) throws SshException, IOException, ShellTimeoutException {
		this(session, OS.code(osType));
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	private ExpectShell(AbstractSessionTask<SessionChannelNG> session, OS osType) throws SshException, IOException, ShellTimeoutException {
		this(session, null, 30000, osType);
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public ExpectShell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger)
			throws SshException,
			IOException, ShellTimeoutException {
		this(session, trigger, 30000);
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public ExpectShell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger, long startupTimeout)
			throws SshException,
			IOException, ShellTimeoutException {
		this(session, trigger, startupTimeout, OS.UNKNOWN);
	}
	
	@Deprecated(forRemoval = true)
	public ExpectShell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger,
			long startupTimeout, int osType)
			throws SshException,
			IOException, ShellTimeoutException {
		this(session, trigger, startupTimeout, OS.code(osType));
	}
	
	@Deprecated(forRemoval = true)
	private ExpectShell(AbstractSessionTask<SessionChannelNG> session, ShellStartupTrigger trigger,
			long startupTimeout, OS osType)
			throws SshException,
			IOException, ShellTimeoutException {
		this(session.getSession(), trigger, startupTimeout, osType);
	}

	@Deprecated(since = "3.2.0", forRemoval = true)	
	public ExpectShell(SessionChannelNG session, int osType) throws SshException, IOException, ShellTimeoutException {
		this(session, OS.code(osType));
	}

	@Deprecated(since = "3.2.0", forRemoval = true)	
	private ExpectShell(SessionChannelNG session, OS osType) throws SshException, IOException, ShellTimeoutException {
		this(session, null, 30000, osType);
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public ExpectShell(SessionChannelNG session, ShellStartupTrigger trigger,
			long startupTimeout, OS osType)
			throws SshException,
			IOException, ShellTimeoutException {

		this.startupTimeout = Duration.ofMillis(startupTimeout);
		this.childShell = false;
		this.characterEncoding = defaultEncoding();
		this.passwordPrompt = DEFAULT_PASSWORD_PROMPT;
		this.passwordErrorText = PASSWORD_ERROR_TEXT;
		
		if(Log.isDebugEnabled())
			Log.debug("Creating session for interactive shell");

		closeHooks.add(() -> session.close());
		
		// Allow the shell to initialize before we start sending data
		if(SHELL_INIT_PERIOD > 0) {
			try {
				Thread.sleep(SHELL_INIT_PERIOD);
			} catch (InterruptedException e) {
			}
		}
		
		if(osType == OS.UNKNOWN) {
			osType = determineServerType(session.getConnection().getRemoteIdentification());
		}

		startupIn = new StartupInputStream(osType, BEGIN_COMMAND_MARKER,
				osType != OS.OPENVMS, trigger, this, session.getInputStream(), session.getOutputStream());
		this.osType = startupIn.osType;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	ExpectShell(InputStream in, OutputStream out, 
			OS osType, ExpectShell parentShell)
			throws SshIOException, SshException, IOException,
			ShellTimeoutException {
		this.characterEncoding = parentShell.characterEncoding;
		this.childShell = true;
		this.passwordPrompt = parentShell.passwordPrompt;
		this.passwordErrorText = parentShell.passwordErrorText;
		this.startupTimeout = parentShell.startupTimeout;

		startupIn = new StartupInputStream(osType, BEGIN_COMMAND_MARKER,
				true, null, this, in, out);
		this.osType = startupIn.osType;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public ExpectShell(InputStream in, OutputStream out, ExpectShell parentShell)
			throws SshIOException, SshException, IOException,
			ShellTimeoutException {
		this(in, out, parentShell, parentShell.osType);
	}

	/**
	 * Deprecated. Will be made private at 3.3.0.
	 * 
	 * @param in in 
	 * @param out out 
	 * @param parentShell parentShell
	 * @param osType os type
	 * @throws SshIOException on SSH IO error
	 * @throws SshException on SSH errror
	 * @throws IOException on IO error
	 * @throws ShellTimeoutException on timeout
	 */
	@Deprecated(since = "3.2.0")
	public ExpectShell(InputStream in, OutputStream out, ExpectShell parentShell, OS osType)
			throws SshIOException, SshException, IOException,
			ShellTimeoutException {
		this.childShell = true;
		this.characterEncoding = parentShell.characterEncoding;
		this.startupTimeout = parentShell.startupTimeout;
		this.passwordPrompt = parentShell.passwordPrompt;
		this.passwordErrorText = parentShell.passwordErrorText;

		startupIn = new StartupInputStream(osType, BEGIN_COMMAND_MARKER,
				false, null, this, in, out);
		this.osType = startupIn.osType;
	}
	
	private ExpectShell(ExpectShellBuilder bldr) throws SshException, IOException, ShellTimeoutException {
		this.childShell = false;
		this.startupTimeout = bldr.startupTimeout;
		this.characterEncoding = bldr.encoding.orElseGet(() -> defaultEncoding());
		this.passwordPrompt = bldr.passwordPrompt.orElse(DEFAULT_PASSWORD_PROMPT);
		this.passwordErrorText = bldr.passwordErrorText.orElse(PASSWORD_ERROR_TEXT);
		
		startupIn = new StartupInputStream(
			bldr.os.orElseGet(() -> 
				bldr.remoteIdentification.or(
						() -> bldr.session.map(sesh -> sesh.getConnection().getRemoteIdentification())).map(this::determineServerType).orElse(OS.UNKNOWN) 
			), 
			BEGIN_COMMAND_MARKER,
			bldr.detectSettings, 
			bldr.trigger.orElse(null), 
			this, 
			bldr.input.or(() -> bldr.session.map(sesh -> sesh.getInputStream())).orElseThrow(() -> new IllegalStateException("InputStream could not be determined.")), 
			bldr.output.or(() -> bldr.session.map(sesh -> sesh.getOutputStream())).orElseThrow(() -> new IllegalStateException("OutputStream could not be determined.")));
		this.osType = startupIn.osType;
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getCharacterEncoding() {
		return characterEncoding.name();
	}

	/**
	 * Set the default character encoding.
	 * <p>
	 * Deprecated for removal. Character encoding will become final,
	 * use the builder option instead.
	 * 
	 * @param characterEncoding character encoding. 
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = Charset.forName(characterEncoding);
	}

	@Deprecated(forRemoval = true, since = "3.1.3")
	public static void setShellInitTimeout(int timeout) {
		SHELL_INIT_PERIOD = timeout;
	}
	
	public InputStream getStartupInputStream() {
		return startupIn;
	}
	
	OS determineServerType(String remoteID)
	{
	    if ( remoteID.indexOf( "OpenVMS" ) > 0 )
	    {
	        return OS.OPENVMS;
	    }
	    
	    if(remoteID.indexOf("Windows") > 0) {
	    	return OS.WINDOWS;
	    }
	    
	    return OS.UNKNOWN;
	}

	public boolean inStartup() {
		return startupIn.inStartup;
	}


	/**
	 * Set the password error text string.
	 * <p>
	 * Deprecated for removal. Password error text will become final,
	 * use the builder option instead.
	 * 
	 * @param passwordErrorText password error text 
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setPasswordErrorText(String passwordErrorText) {
		this.passwordErrorText = passwordErrorText;
	}

	/**
	 * Set the password prompt string.
	 * <p>
	 * Deprecated for removal. Password prompt will become final,
	 * use the builder option instead.
	 * 
	 * @param passwordPrompt password prompt 
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setPasswordPrompt(String passwordPrompt) {
		this.passwordPrompt = passwordPrompt;
	}
	
	public ShellReader getStartupReader() {
		return startupIn.startupController;
	}

	public ExpectShell su(String cmd, String password) throws SshIOException,
			SshException, IOException, ShellTimeoutException {
		return su(cmd, password, passwordPrompt, new ShellDefaultMatcher());
	}

	public ExpectShell su(String cmd, String password, String passwordPrompt)
			throws SshException, SshIOException, IOException,
			ShellTimeoutException {
		return su(cmd, password, passwordPrompt, new ShellDefaultMatcher());
	}

	public ExpectShell su(String cmd) throws SshException, SshIOException,
			IOException, ShellTimeoutException {
		ShellProcess process = executeCommand(cmd, false, false);
		return new ExpectShell(process.getInputStream(), process.getOutputStream(),
				osType, this);
	}

	public ExpectShell su(String cmd, String password, String passwordPrompt,
			ShellMatcher matcher) throws SshException, SshIOException,
			IOException, ShellTimeoutException {
		ShellProcess process = executeCommand(cmd, false, false);
		ShellProcessController contr = new ShellProcessController(process,
				matcher);
		process.mark(1024);
		if (contr.expectNextLine(passwordPrompt)) {
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
			return new ExpectShell(process.getInputStream(),
					process.getOutputStream(), osType, this);
		} else {
			throw new SshException("The command failed: " + cmd,
					SshException.SHELL_ERROR);
		}
	}

	public ShellProcess sudo(String cmd, String password) throws SshException,
			ShellTimeoutException, IOException {
		return sudo(cmd, password, passwordPrompt, new ShellDefaultMatcher());
	}
	
	public ShellProcess sudo(String cmd) throws SshException,
	ShellTimeoutException, IOException {
		return sudo(cmd, sudoPassword.orElseThrow(), passwordPrompt, new ShellDefaultMatcher());
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
		process.mark(4096);
		int lines = 0;
		boolean found;
		do {
			found = contr.expectNextLine(promptExpression);
		} while(!found && ++lines < 10 && !contr.isEOF());
		
		if(found) {
			if(Log.isDebugEnabled())
				Log.debug("sudo password expression matched");
			contr.typeAndReturn(password);
			if(contr.expectNextLine(passwordErrorText)) {
				throw new IOException("Incorrect password!");
			}
			process.clearOutput();
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

	public void exit() throws IOException, SshException {
		startupIn.sessionOut.write(("exit" + osType.eol()).getBytes());
		if (childShell) {
			while (startupIn.sessionIn.read() > -1)
				;
		}
		close();
	}

	public void close() throws IOException, SshException {
		internalClose();
	}

	public String getNewline() {
		return osType.eol();
	}

	public synchronized String executeWithOutput(String cmd) throws SshException {
		return executeCommand(cmd, true).getCommandOutput();
	}
	
	public synchronized int executeWithExitCode(String cmd) throws SshException {
		return executeCommand(cmd, true).getExitCode();
	}
	
	public synchronized void execute(String cmd) throws SshException {
		executeCommand(cmd, true);
	}
	
	public synchronized ShellProcess executeCommand(String origCmd)
			throws SshException {
		return executeCommand(origCmd, false, false, null);
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			boolean consume) throws SshException {
		return executeCommand(origCmd, false, consume, null);
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
		return executeCommand(origCmd, matchPromptMarker, consume, null);
	}

	public synchronized ShellProcess executeCommand(String origCmd,
			boolean matchPromptMarker, boolean consume, String charsetName)
			throws SshException {

		try {
			Charset charset = charsetName == null ? characterEncoding : Charset.forName(charsetName);
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
					| ((origCmd.startsWith(".") || origCmd.startsWith("source")) && osType == OS.HPUX);

			if (matchPromptMarker) {

				// Get the prompt value
				startupIn.sessionOut.write(osType.eol().getBytes());
				startupIn.sessionOut.write(osType.eol().getBytes());

				int ch;
				while ((ch = startupIn.sessionIn.read()) > -1 && ch != '\n')
					;

				while ((ch = startupIn.sessionIn.read()) > -1 && ch != '\n')
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

			if (osType == OS.WINDOWS) {
				// %errorlevel% doesnt work properly on multiple command line so
				// we fix it to 0 and 1 for good or bad result
			    echoCmd = osType.echoCommand() + " " + BEGIN_COMMAND_MARKER + " && " + cmd
		                		+ " && " + osType.echoCommand() + " " + endCommand + "0"
						+ " || " + osType.echoCommand() + " " + endCommand + "1" + osType.eol();
			} else if ( osType == OS.OPENVMS ) {
			    // Do same trick with end marker for OpenVMS via its PIPE command.
				echoCmd = osType.pipeCommand() + osType.echoCommand() + " \"" + BEGIN_COMMAND_MARKER + "\" && " + cmd
		                		+ " && " + osType.echoCommand() + " \"" + endCommand + "0\" || "
		                		+ osType.echoCommand() + "\"" + endCommand + "1\"" + osType.eol();
			} else if(osType == OS.POWERSHELL) {
			    // Force powershell format
                echoCmd = "echo \"" + BEGIN_COMMAND_MARKER + "\"; " + cmd
                        + "; echo \"" + endCommand + osType.exitCodeVariable() + "\"" + osType.eol();
			} else {
			    // Assume it's a Unix system and 'echo' works.
                echoCmd = "echo \"" + BEGIN_COMMAND_MARKER + "\"; " + cmd
                        + "; echo \"" + endCommand + osType.exitCodeVariable() + "\"" + osType.eol();
			}
			
			if(Log.isDebugEnabled()) {
				Log.debug("Executing raw command: {}", echoCmd);
			}
			
			startupIn.sessionOut.write(echoCmd.getBytes(charset));

			numCommandsExecuted++;

			ShellInputStream in  = new ShellInputStream(
					startupIn.sessionIn,
					this, BEGIN_COMMAND_MARKER, endCommand, origCmd,
					matchPromptMarker, prompt.toString().trim());
			ShellProcess process = new ShellProcess(this, in, startupIn.sessionOut);

			if (consume) {
				process.waitFor();
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
		return osType.code();
	}

	public String getOsDescription() {
		return osType.description();
	}

	/**
	 * Type some characters as input on the remote shell.
	 * 
	 * @param string
	 *            String
	 * @throws IOException
	 */
	void type(String string) throws IOException {
		write(string.getBytes(characterEncoding));
	}

	/**
	 * Write some data as input on the remote shell.
	 * 
	 * @param bytes
	 * @throws IOException
	 */
	void write(byte[] bytes) throws IOException {
		startupIn.sessionOut.write(bytes);
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
		write(osType.eol().getBytes(characterEncoding));
	}

	/**
	 * Type some characters followed by a carriage return on the remote shell.
	 * 
	 * @param string
	 *            String
	 * @throws IOException
	 */
	void typeAndReturn(String string) throws IOException {
		write((string + osType.eol()).getBytes(characterEncoding));
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

	static class StartupInputStream extends InputStream {

		final char[] marker1;
		int markerPos;
		final StringBuilder currentLine = new StringBuilder();
		final boolean detectSettings;
		final ShellController startupController;
		final BufferedInputStream sessionIn;
		final OutputStream sessionOut;
		final ExpectShell shell;
		final Instant startupStarted = Instant.now();

		OS osType;
		boolean inStartup;

		StartupInputStream(
				OS osType,
				String marker1str, boolean detectSettings,
				ShellStartupTrigger trigger, ExpectShell shell,
				InputStream in, OutputStream out) throws SshException, IOException,
				ShellTimeoutException {
			
			this.osType = osType;
			this.shell = shell;
			
			sessionIn = new BufferedInputStream(in);
			sessionOut = out;
			
			this.detectSettings = detectSettings;
			this.marker1 = marker1str.toCharArray();

			startupController = new ShellController(shell,
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

			    String cmd;

			    if (osType == OS.WINDOWS) {
					// %errorlevel% doesnt work properly on multiple command line so
					// we fix it to 0 and 1 for good or bad result
				    cmd = osType.echoCommand() + " " + BEGIN_COMMAND_MARKER + "&& " + osType.echoCommand() + " " + osType.exitCodeVariable() + osType.eol();
				} else if ( osType == OS.OPENVMS ) {
				    // Do same trick with end marker for OpenVMS via its PIPE command.
					cmd = osType.pipeCommand() + osType.echoCommand() + " \"" + BEGIN_COMMAND_MARKER + "\" && " + osType.echoCommand() + " $?" + osType.eol();
				} else if(osType == OS.POWERSHELL) {
				    // Force powershell format
	                cmd = "echo \"" + BEGIN_COMMAND_MARKER + "\"; "
	                        + "; echo \"" + osType.exitCodeVariable() + "\"" + osType.eol();
				} else {
				    // Assume it's a Unix system and 'echo' works.
	                cmd = "echo \"" + BEGIN_COMMAND_MARKER + "\"; " + "echo \"$?\"" + osType.eol();
				}			    
			    
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
						if (System.currentTimeMillis() - startupStarted.toEpochMilli() > shell.startupTimeout.toMillis())
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
							if (System.currentTimeMillis() - startupStarted.toEpochMilli() > shell.startupTimeout.toMillis())
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
					currentLine.setLength(0);
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
			if (line.equals("0") && osType == OS.UNKNOWN) {
			    if(Log.isDebugEnabled())
					Log.debug("This looks like a *nix type machine, setting EOL to CR only and exit code variable to $?");

				// Attempt to execute uname for some information
				ShellProcess proc = shell.executeCommand("uname");
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
						osType = OS.SOLARIS;
					} else if (line.startsWith("aix")) {
						osType = OS.AIX;
					} else if (line.startsWith("darwin")) {
						osType = OS.DARWIN;
					} else if (line.startsWith("freebsd")) {
						osType = OS.FREEBSD;
					} else if (line.startsWith("openbsd")) {
						osType = OS.OPENBSD;
					} else if (line.startsWith("netbsd")) {
						osType = OS.NETBSD;
					} else if (line.startsWith("linux")) {
						osType = OS.LINUX;
					} else if (line.startsWith("hp-ux")) {
						osType = OS.HPUX;
					} else {
						osType = OS.UNKNOWN;
					}
					break;
				case 127:
					Log.debug("Remote side does not support uname");
					break;
				default:
					Log.debug("uname returned error code " + proc.getExitCode());
				}

			} else if (osType == OS.UNKNOWN) {
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
					osType = OS.WINDOWS;
				}
			}

			switch(osType) {
			case WINDOWS:
			case OPENVMS:
			case POWERSHELL:
				break;
			default:
				if(Log.isDebugEnabled())
					Log.debug("Setting default sudo prompt");
				
				shell.executeCommand("export SUDO_PROMPT=Password:", true);
				break;
			}

			
			if(Log.isDebugEnabled())
				Log.debug("Shell initialized");
		}

	}

	private static Charset defaultEncoding() {
		return Charset.forName("UTF-8");
	}
	
	public void setSudoPassword(Optional<String> sudoPassword) {
		this.sudoPassword = sudoPassword;
	}

}
