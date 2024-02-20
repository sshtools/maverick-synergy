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

import java.util.Optional;
import java.util.function.Function;

import com.sshtools.client.SshClient;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.Connection;

public abstract class AbstractConnectionTask extends Task {
	public static abstract class AbstractConnectionTaskBuilder<B extends AbstractConnectionTaskBuilder<B, T>, T extends AbstractConnectionTask> {

		protected Optional<Function<Integer, SshClient>> clientSupplier = Optional.empty();
		protected Optional<SshConnection> connection = Optional.empty();
		
		/**
		 * Set a single {@link SshClient} to use.
		 * 
		 * @param client client
		 * @return builder for chaining
		 * 
		 */
		public B withClient(SshClient client) {
			return withClients((idx) -> client);
		}
		
		/**
		 * Set a single {@link Connection} to use.
		 * 
		 * @param connection connection
		 * @return builder for chaining
		 * 
		 */
		@SuppressWarnings("unchecked")
		public B withConnection(SshConnection connection) {
			this.connection = Optional.of(connection);
			return (B) this;
		}

		/**
		 * Set a {@link Function} that supplies
		 * {@link SshClient} instances. The function is provided with an
		 * <code>index</code>, with the control client being index <code>0</code>, and
		 * subsequent clients having index of <code>1</code> up to to whatever
		 * {@link #withChunks(int)} was given (or the default of <code>3</code>).
		 * 
		 * @param clientSupplier supplier of clients
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public B withClients(Function<Integer, SshClient> clientSupplier) {
			this.clientSupplier = Optional.of(clientSupplier);
			return (B) this;
		}
		
		public abstract T build();
	}
	
	protected final Optional<Function<Integer, SshClient>> clientSupplier;

	protected AbstractConnectionTask(AbstractConnectionTaskBuilder<?, ?> builder) {
		super(builder.connection.orElse(builder.clientSupplier.orElseThrow(() -> new IllegalArgumentException("No connection or client supplied.")).apply(0).getConnection()));
		this.clientSupplier = builder.clientSupplier;
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	protected AbstractConnectionTask(SshClient ssh) {
		super(ssh);
		clientSupplier = Optional.empty();
	}

	@Deprecated(since = "3.1.0", forRemoval = true)
	protected AbstractConnectionTask(SshConnection con) {
		super(con);
		clientSupplier = Optional.empty();
	}

}
