package com.sshtools.client.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Optional;

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
	}
	
}
