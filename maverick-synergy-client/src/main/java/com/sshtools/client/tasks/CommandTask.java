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
import java.nio.charset.Charset;
import java.util.Optional;

import com.sshtools.client.PseudoTerminalModes;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.common.ssh.SshConnection;

/**
 * Task for executing commands.
 */
@SuppressWarnings("deprecation")
public final class CommandTask extends AbstractCommandTask {
	
	/**
	 * Functional interface for tasks run on certain command task events.
	 */
	@FunctionalInterface
	public interface CommandTaskEvent {
		/**
		 * Command task event occurred. Checked exceptions are caught and rethrown as an
		 * {@link IllegalStateException}.
		 * 
		 * @param task task
		 * @param session session
		 * @throws Exception on any error
		 */
		void commandEvent(CommandTask task, SessionChannelNG session) throws Exception;
	}

	/**
	 * Builder for {@link CommandTask}.
	 */
	public final static class CommandTaskBuilder extends AbstractSessionTaskBuilder<CommandTaskBuilder, SessionChannelNG, CommandTask> {

		private Optional<CommandTaskEvent> onClose = Optional.empty();
		private Optional<CommandTaskEvent> onBeforeExecute = Optional.empty();
		private Optional<CommandTaskEvent> onBeforeTask = Optional.empty();
		private Optional<CommandTaskEvent> onTask = Optional.empty();
		private Optional<Charset> encoding = Optional.empty();
		private Optional<String> command = Optional.empty();
		private Optional<String> termType = Optional.empty();
		private int cols = 80;
		private int rows = 24;
		private boolean withPty = true;
		private Optional<PseudoTerminalModes> modes = Optional.empty();
		private boolean autoConsume;
		
		private CommandTaskBuilder() {
		}
		
		/**
		 * Set to auto-consume input. Will be ignored if {@link #withSession(java.util.function.Function)}
		 * has been used.
		 * 
		 * @return this for chaining
		 */
		public CommandTaskBuilder withAutoConsume() {
			return withAutoConsume(true);
		}
		
		/**
		 * Set the whether to auto-consume input. Will be ignored if {@link #withSession(java.util.function.Function)}
		 * has been used.
		 * 
		 * @param autoConsume auto consume
		 * @return this for chaining
		 */
		public CommandTaskBuilder withAutoConsume(boolean autoConsume) {
			this.autoConsume = autoConsume;
			return this;
		}
		
		/**
		 * Set the terminal type to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param term type
		 * @return builder for chaining
		 */
		public final CommandTaskBuilder withTermType(String termType) {
			return withTermType(Optional.of(termType));
		}

		/**
		 * Set the terminal type to use when allocating a PTY. Note, this
		 * will have no effect if {{@link #withPty} is set to <code>false</code>.
		 * 
		 * @param term type
		 * @return builder for chaining
		 */
		public final CommandTaskBuilder withTermType(Optional<String> termType) {
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
		public final CommandTaskBuilder withColumns(int cols) {
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
		public final CommandTaskBuilder withRows(int rows) {
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
		public final CommandTaskBuilder withModes(PseudoTerminalModes modes) {
			this.modes = Optional.of(modes);
			return this;
		}


		/**
		 * Set whether or not to allocate a Pty. When set to <code>true</code>, other
		 * Pty characteristics can be set using builder methods such as {@link #withRows(int)}, 
		 * {@link #withModes(PseudoTerminalModes)} and others. By default a Pty is allocated.
		 * 
		 * @param withPty whether to allocate a PTY. 
		 * @return builder for chaining
		 */
		public CommandTaskBuilder withPty(boolean withPty) {
			this.withPty = withPty;
			return this;
		}

		/**
		 * Do not to allocate a Pty. 
		 * 
		 * @param withPty whether to allocate a PTY. 
		 * @return builder for chaining
		 */
		public CommandTaskBuilder withoutPty() {
			this.withPty = false;
			return this;
		}
		
		/**
		 * Set the command to run. This is mandatory.
		 * 
		 * @param command command
		 * @return this for chaining
		 */
		public CommandTaskBuilder withCommand(String command) {
			this.command = Optional.of(command);
			return this;
		}
		
		/** 
		 * Set the character encoding to use for transferring string content.
		 * 
		 * @param encoding encoding 
		 * @return builder for chaining
		 */
		public CommandTaskBuilder withEncoding(String encoding) {
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
		public CommandTaskBuilder withEncoding(Charset encoding) {
			this.encoding = Optional.of(encoding);
			return this;
		} 

		/**
		 * Create a new {@link CommandTaskBuilder}.
		 * 
		 * @return builder
		 */
		public static CommandTaskBuilder create() {
			return new CommandTaskBuilder();
		}

		/**
		 * Set a callback to run before the command is executed.
		 * 
		 * @param onBeforeExecute on before execute callback
		 * @return builder for chaining
		 */
		public final CommandTaskBuilder onBeforeExecute(CommandTaskEvent onBeforeExecute) {
			this.onBeforeExecute = Optional.of(onBeforeExecute);
			return this;
		}

		/**
		 * Set a callback to run when the command is closed. 
		 * 
		 * @param onClose on close callback
		 * @return builder for chaining
		 */
		public final CommandTaskBuilder onClose(CommandTaskEvent onClose) {
			this.onClose = Optional.of(onClose);
			return this;
		}

		/**
		 * Set a callback to run when the command has been executed. This 
		 * should NOT block until the task is done. 
		 * 
		 * @param onOpen on session channel open
		 * @return builder for chaining
		 */
		public final CommandTaskBuilder onBeforeTask(CommandTaskEvent onOpen) {
			this.onBeforeTask = Optional.of(onOpen);
			return this;
		}

		/**
		 * Set a callback to run when the command has been executed. Here you can obtain 
		 * I/O streams if required, and then block until the task is done. The channel 
		 * backing this task will be closed when the callback exits. If you do not
		 * set this, {@link CommandTask#close()} should be called when the task
		 * is finished with.
		 * 
		 * @param onExecute execute task
		 * @return builder for chaining
		 */
		public final CommandTaskBuilder onTask(CommandTaskEvent onExecute) {
			this.onTask = Optional.of(onExecute);
			return this;
		}

		@Override
		public CommandTask build() {
			return new CommandTask(this);
		}
	}
	
	private final Optional<CommandTaskEvent> onClose;
	private final Optional<CommandTaskEvent> onBeforeExecute;
	private final Optional<CommandTaskEvent> onBeforeTask;
	private final Optional<CommandTaskEvent> onTask;
	private final String termType;
	private final int rows;
	private final int cols;
	private final boolean withPty;
	private final Optional<PseudoTerminalModes> modes;
	
	private CommandTask(CommandTaskBuilder builder) {
		super(builder, 
			builder.command.orElseThrow(() -> new IllegalArgumentException("Command must be supplied")),
			builder.encoding.map(Charset::name).orElse("UTF-8"),
			builder.autoConsume
		);
		this.onClose = builder.onClose;
		this.onBeforeExecute = builder.onBeforeExecute;
		this.onTask = builder.onTask;
		this.onBeforeTask = builder.onBeforeTask;
		this.withPty = builder.withPty;
		this.termType = builder.termType.orElse("dumb");
		this.rows = builder.rows;
		this.cols = builder.cols;
		this.modes = builder.modes;
	}
	
	@Override
	@Deprecated(since = "3.1.0", forRemoval = true)
	protected final void onCloseSession(SessionChannelNG session) {
		super.onCloseSession(session);
		onClose.ifPresent(c -> {
			try {
				c.commandEvent(this, session);
			} catch(RuntimeException re) {
				throw re;
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		});
	}

	@Override
	public final int getExitCode() {
		return super.getExitCode();
	}

	@Override
	public final String getCommand() {
		return super.getCommand();
	}
	
	@SuppressWarnings("removal")
	@Override
	protected final SessionChannelNG createSession(SshConnection con) {
		return super.createSession(con);
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	@Override
	protected void closeOnTaskComplete() {
		/* Noop, new style tasks should either be closed manually or in the onExec() handler */
	}

	@Override
	protected final void onOpenSession(SessionChannelNG session) throws IOException {
		onBeforeTask.ifPresent(c -> {
			try {
				c.commandEvent(this, session);
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
				c.commandEvent(this, session);
			} catch(RuntimeException re) {
				throw re;
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			} finally {
				close();
			}
		});
	}

	protected final void beforeExecuteCommand(SessionChannelNG session) {
		
		try {
			if(withPty) {
				if(modes.isEmpty())
					session.allocatePseudoTerminal(termType, cols, rows);
				else
					session.allocatePseudoTerminal(termType, cols, rows, 0, 0, modes.get());
			}
			
			onBeforeExecute.ifPresent(c -> {
				try {
					c.commandEvent(this, session);
				} catch(RuntimeException re) {
					throw re;
				} catch(IOException ioe) {
					throw new UncheckedIOException(ioe);
				} catch (Exception e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			});
			
		} catch(RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		

	}
	
}
