package com.sshtools.client.tasks;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import com.sshtools.client.PseudoTerminalModes;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.ssh.TerminalModes;

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
	public final static class ShellTaskBuilder extends AbstractSessionTaskBuilder<ShellTaskBuilder, SessionChannelNG, ShellTask> {

		private Optional<ShellTaskEvent> onClose = Optional.empty();
		private Optional<ShellTaskEvent> onBeforeOpen = Optional.empty();
		private Optional<ShellTaskEvent> onBeforeTask = Optional.empty();
		private Optional<ShellTaskEvent> onTask = Optional.empty();
		private Optional<String> termType = Optional.empty();
		private int cols = 80;
		private int rows = 24;
		private boolean withPty = true;
		private Optional<TerminalModes> modes = Optional.empty();
		private boolean autoConsume;

		private ShellTaskBuilder() {
		}
		
		/**
		 * Set to auto-consume input. Will be ignored if {@link #withSession(java.util.function.Function)}
		 * has been used.
		 * 
		 * @return this for chaining
		 */
		public ShellTaskBuilder withAutoConsume() {
			return withAutoConsume(true);
		}
		
		/**
		 * Set the whether to auto-consume input. Will be ignored if {@link #withSession(java.util.function.Function)}
		 * has been used.
		 * 
		 * @param autoConsume auto consume
		 * @return this for chaining
		 */
		public ShellTaskBuilder withAutoConsume(boolean autoConsume) {
			this.autoConsume = autoConsume;
			return this;
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
		@Deprecated(since = "3.1.2", forRemoval = true)
		public final ShellTaskBuilder withModes(PseudoTerminalModes modes) {
			return withModes(TerminalModes.TerminalModesBuilder.create().fromBytes(modes.toByteArray()).build());
		}

		/**
		 * Set the terminal modes to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param modes modes
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder withModes(TerminalModes modes) {
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
		 * Set a callback to run when the command has been executed. This 
		 * should NOT block until the task is done. 
		 * .
		 * 
		 * @param onBeforeTask on session channel open
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder onBeforeTask(ShellTaskEvent onBeforeTask) {
			this.onBeforeTask = Optional.of(onBeforeTask);
			return this;
		}

		/**
		 * Set a callback to run when the command has been executed. Here you can obtain 
		 * I/O streams if required, and then block until the task is done. The channel 
		 * backing this task will be closed when the callback exits. If you do not
		 * set this, {@link CommandTask#close()} should be called when the task
		 * is finished with.
		 * 
		 * @param onTask execute task
		 * @return builder for chaining
		 */
		public final ShellTaskBuilder onTask(ShellTaskEvent onTask) {
			this.onTask = Optional.of(onTask);
			return this;
		}

		/**
		 * Use {@link #onTask(ShellTaskEvent)}.
		 */
		@Deprecated
		public final ShellTaskBuilder onOpen(ShellTaskEvent onTask) {
			return onTask(onTask);
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
	private final Optional<ShellTaskEvent> onBeforeTask;
	private final Optional<ShellTaskEvent> onTask;
	private final String termType;
	private final int rows;
	private final int cols;
	private final boolean withPty;
	private final Optional<TerminalModes> modes;
	private final boolean autoConsume;
	
	private ShellTask(ShellTaskBuilder builder) {
		super(builder);
		this.onClose = builder.onClose;
		this.onStartShell = builder.onBeforeOpen;
		this.onBeforeTask = builder.onBeforeTask;
		this.onTask = builder.onTask;
		this.withPty = builder.withPty;
		this.termType = builder.termType.orElse("dumb");
		this.rows = builder.rows;
		this.cols = builder.cols;
		this.modes = builder.modes;
		this.autoConsume = builder.autoConsume;
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
	@Deprecated(since = "3.1.0")
	protected void onOpenSession(SessionChannelNG session) throws IOException, SshException, ShellTimeoutException {
		onBeforeTask.ifPresent(c -> {
			try {
				c.shellEvent(this, session);
			} catch(RuntimeException re) {
				throw re;
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			} 
		});
		onTask.ifPresent(c -> {
			try {
				c.shellEvent(this, session);
			} catch(RuntimeException re) {
				throw re;
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe.getMessage(), ioe);
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			} finally {
				close();
			}
		});
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	@Override
	protected void closeOnTaskComplete() {
		/* Noop, new style tasks should either be closed manually or in the onExec() handler */
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
		return new SessionChannelNG(
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxPacketSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMaxWindowSize(),
				con.getContext().getPolicy(ShellPolicy.class).getSessionMinWindowSize(),
				getChannelFuture(),
				autoConsume);
	}
}
