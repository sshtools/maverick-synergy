package com.sshtools.client.tasks;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import com.sshtools.client.PseudoTerminalModes;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;

/**
 * A {@link Task} that starts a remote shell with an allocated PTY.
 * You cannot directly create a {@link ShellTask}, instead use {@link ShellTaskBuilder}.
 * <pre>
 * client.addTask(ShellTaskBuilder.create().
 *      withTermType("vt320").
 *      withColumns(132).
 *      withRows(48).
 *      onClose((task, session) -> System.out.println("Closed!")).
 *      build());
 * </pre>
 *
 */
public class ShellTask extends AbstractShellTask<SessionChannelNG> {
	
	/**
	 * Functional interface for tasks run on certain shell events.
	 */
	@FunctionalInterface
	public interface ShellTaskEvent {
		/**
		 * Shell event occurred. Checked exceptions are caught and rethrown as an
		 * {@link IllegalStateException}.
		 * 
		 * @param task task
		 * @param session session
		 * @throws Exception on any error
		 */
		void shellEvent(ShellTask task, SessionChannelNG session) throws Exception;
	}

	/**
	 * Builder for {@link ShellTask}.
	 */
	public final static class ShellTaskBuilder extends AbstractConnectionTaskBuilder<ShellTaskBuilder, ShellTask> {

		private Optional<ShellTaskEvent> onClose = Optional.empty();
		private Optional<ShellTaskEvent> onBeforeOpen = Optional.empty();
		private Optional<ShellTaskEvent> onOpen = Optional.empty();
		private Optional<String> termType = Optional.empty();
		private Optional<Function<SshConnection, SessionChannelNG>> session = Optional.empty();
		private int cols = 80;
		private int rows = 24;
		private boolean withPty = true;
		private Optional<PseudoTerminalModes> modes = Optional.empty();

		private ShellTaskBuilder() {
		}

		/**
		 * Create a new {@link ShellTaskBuilder}.
		 * 
		 * @return builder
		 */
		public static ShellTaskBuilder create() {
			return new ShellTaskBuilder();
		}

		/**
		 * Set a function to create a custom session channel.
		 * 
		 * @param session session function
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withSession(Function<SshConnection, SessionChannelNG> session) {
			this.session = Optional.of(session);
			return this;
		}

		/**
		 * Set the terminal type to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param term type
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withTermType(String termType) {
			return withTermType(Optional.of(termType));
		}

		/**
		 * Set the terminal type to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param term type
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withTermType(Optional<String> termType) {
			this.termType = termType;
			return this;
		}

		/**
		 * Set the terminal width in columns to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param cols cols
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withColumns(int cols) {
			this.cols = cols;
			return this;
		}

		/**
		 * Set the terminal height in rows to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param rows row
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withRows(int rows) {
			this.rows = rows;
			return this;
		}

		/**
		 * Set the terminal modes to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param modes modes
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withModes(PseudoTerminalModes modes) {
			this.modes = Optional.of(modes);
			return this;
		}

		/**
		 * Set a callback to run before the opening of the shell. By default, this will allocate a new
		 * PTY using the other configuration in this builder, such as terminal type, columns etc. The pty
		 * is allocated before this callback is called.
		 * 
		 * @param onStartShell on start shell callback
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder onBeforeOpen(ShellTaskEvent onStartShell) {
			this.onBeforeOpen = Optional.of(onStartShell);
			return this;
		}

		/**
		 * Set a callback to run when the shell is closed. 
		 * 
		 * @param onBeforeOpen on start shell callback
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder onClose(ShellTaskEvent onClose) {
			this.onClose = Optional.of(onClose);
			return this;
		}

		/**
		 * Set a callback to run when the shell channel is opened. 
		 * 
		 * @param onOpen on start shell open
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder onOpen(ShellTaskEvent onOpen) {
			this.onOpen = Optional.of(onOpen);
			return this;
		}

		@Override
		public ShellTask build() {
			return new ShellTask(this);
		}

		/**
		 * Set whether or not to allocate a Pty. When set to <code>true</code>, other
		 * Pty characteristics can be set using builder methods such as {@link #withRows(int)}, 
		 * {@link #withModes(PseudoTerminalModes)} and others. By default a Pty is allocated.
		 * 
		 * @param withPty whether to allocate a PTY. 
		 * @return builder for chaining
		 */
		public ShellTaskBuilder withPty(boolean withPty) {
			this.withPty = withPty;
			return this;
		}

		/**
		 * Do not to allocate a Pty. 
		 * 
		 * @param withPty whether to allocate a PTY. 
		 * @return builder for chaining
		 */
		public ShellTaskBuilder withoutPty() {
			this.withPty = false;
			return this;
		}
	}

	private final Optional<ShellTaskEvent> onClose;
	private final Optional<ShellTaskEvent> onStartShell;
	private final Optional<ShellTaskEvent> onOpen;
	private final String termType;
	private final int rows;
	private final int cols;
	private final Optional<Function<SshConnection, SessionChannelNG>> session;
	private final boolean withPty;
	private final Optional<PseudoTerminalModes> modes;
	
	private ShellTask(ShellTaskBuilder builder) {
		super(builder);
		this.onClose = builder.onClose;
		this.onStartShell = builder.onBeforeOpen;
		this.onOpen = builder.onOpen;
		this.withPty = builder.withPty;
		this.termType = builder.termType.orElse("dumb");
		this.rows = builder.rows;
		this.cols = builder.cols;
		this.session = builder.session;
		this.modes = builder.modes;
	}

	/**
	 * Construct a shell task. Deprecated since 3.1.0. Use a {@link ShellTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @deprecated 
	 * @see ShellTaskBuilder
	 */
	@Deprecated
	public ShellTask(SshConnection con) {
		this(ShellTaskBuilder.create().withConnection(con));
	}

	/**
	 * Construct a shell task. Deprecated since 3.1.0. Use a {@link ShellTaskBuilder} instead. 
	 * 
	 * @param ssh client
	 * @deprecated 
	 * @see ShellTaskBuilder
	 */
	@Deprecated
	public ShellTask(SshClient ssh) {
		this(ShellTaskBuilder.create().withClient(ssh));
	}

	/**
	 * Deprecated for overriding, will be made final at 3.2.0.
	 */
	@Override
	protected void onOpenSession(SessionChannelNG session) throws IOException, SshException, ShellTimeoutException {
		if(onOpen.isPresent()) {
			try {
				onOpen.get().shellEvent(this, session);
			} catch(RuntimeException re) {
				throw re;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * Deprecated for overriding, will be made final at 3.2.0.
	 */
	@Override
	@Deprecated(since = "3.1.0")
	protected void beforeStartShell(SessionChannelNG session) {
		try {
			if(withPty) {
				if(modes.isEmpty())
					session.allocatePseudoTerminal(termType, cols, rows);
				else
					session.allocatePseudoTerminal(termType, cols, rows, 0, 0, modes.get());
			}
			
			if(onStartShell.isPresent()) {
				onStartShell.get().shellEvent(this, session);
			}
			
		} catch(RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Deprecated for overriding, will be made final at 3.2.0.
	 */
	@Override
	@Deprecated(since = "3.1.0")
	protected void onCloseSession(SessionChannelNG session) {
		if(onClose.isPresent()) {
			try {
				onClose.get().shellEvent(this, session);
			} catch(RuntimeException re) {
				throw re;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * Deprecated for overriding, will be made final at 3.2.0.
	 */
	@Override
	@Deprecated(since = "3.1.0")
	protected SessionChannelNG createSession(SshConnection con) {
		return session.orElse((c) -> new SessionChannelNG(
				c.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(),
				c.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				c.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				c.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				getChannelFuture(),
				false)).apply(con);
	}
}
