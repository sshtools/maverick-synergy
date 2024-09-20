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

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sshtools.client.AbstractSessionChannel;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;

/**
 * An abstract task for using the SSH session
 */
public abstract class AbstractSessionTask<T extends AbstractSessionChannel> extends AbstractConnectionTask implements Closeable {

	/**
	 * Builder for {@link AbstractSessionTask}.
	 */
	protected static abstract class AbstractSessionTaskBuilder<
			B extends AbstractSessionTaskBuilder<B, SC, TT>,
			SC extends AbstractSessionChannel,
			TT extends AbstractSessionTask<SC>> extends AbstractConnectionTaskBuilder<B, TT> { 

		private Optional<Function<SshConnection, SC>> session = Optional.empty();
		private Optional<ChannelRequestFuture> future = Optional.empty();
		private Optional<Consumer<SC>> onClose = Optional.empty();

		/**
		 * Set a function to create a custom session channel.
		 * 
		 * @param session session function
		 * @return builder for chaining
		 */
		public final AbstractSessionTaskBuilder<B, SC, TT> withSession(Function<SshConnection, SC> session) {
			this.session = Optional.of(session);
			return this;
		}

		/**
		 * Set a custom {@link ChannelRequestFuture}.
		 * 
		 * @param future future
		 * @return builder for chaining
		 */
		public final AbstractSessionTaskBuilder<B, SC, TT> withFuture(ChannelRequestFuture future) {
			this.future  = Optional.of(future);
			return this;
		}

		/**
		 * Set a callback to run when the session is closed. 
		 * 
		 * @param onClose on close callback
		 * @return builder for chaining
		 */
		public final AbstractSessionTaskBuilder<B, SC, TT> onClose(Consumer<SC> onClose) {
			this.onClose = Optional.of(onClose);
			return this;
		}
	}
	
	private long timeout = 10000;
	private final ChannelRequestFuture future;
	private final Optional<T> session;
	private final Optional<Consumer<T>> onClose;
	
	public AbstractSessionTask(AbstractSessionTaskBuilder<?, T, ?> builder) {
		super(builder);
		session = builder.session.map(f -> f.apply(con));
		future = builder.future.orElseGet(() -> new ChannelRequestFuture());
		onClose = builder.onClose;
	}

	public T getSession() {
		return session.get();
	}
	
	public void disconnect() {
		con.disconnect();
	}
	
	public ChannelRequestFuture getChannelFuture() {
		return future;
	}
	
	@Override
	public void doTask() {
		var session = getSession();
		
		con.openChannel(session);
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException("Could not open session channel");
		}
		
		setupSession(session);
	

		try {
			if(Log.isDebugEnabled()) {
				Log.debug("Starting session task");
			}
			onOpenSession(session);
		} catch(Throwable ex) {
			this.lastError = ex;
		}
	}
	
	protected abstract T createSession(SshConnection con);
	
	protected abstract void setupSession(T session);
	
	protected abstract void onOpenSession(T session) throws IOException, SshException, ShellTimeoutException;

	protected abstract void onCloseSession(T session);

	@Override
	public final void close() {
		if(Log.isDebugEnabled()) {
			Log.debug("Ending session task");
		}
		
		var session = getSession();
		session.close();
		
		onCloseSession(session);
		onClose.ifPresent(oc -> oc.accept(session));
		
		done(Objects.isNull(lastError));
		
		if(Log.isDebugEnabled()) {
			Log.debug("Session task is done success={}", String.valueOf(Objects.isNull(lastError)));
		}
	}

	public boolean isClosed() {
		return getSession().isClosed();
	}

	public void changeTerminalDimensions(int cols, int rows, int width, int height) {
		getSession().changeTerminalDimensions(cols, rows, width, height);
	}
	
}
