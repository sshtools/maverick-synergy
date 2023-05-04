/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.client.tasks;

import java.nio.file.Path;
import java.util.Optional;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.synergy.ssh.Connection;

/**
 * An SFTP {@link Task} that uploads complete files.
 * You cannot directly create a {@link StatTask}, instead use {@link StatTaskBuilder}.
 * <pre>
 * client.addTask(StatTaskBuilder.create().
 *      withPath("/path/on/remote/remote.txt").
 *      build());
 * </pre>
 *
 */
public class StatTask extends AbstractFileTask {

	/**
	 * Builder for {@link StatTask}.
	 */
	public final static class StatTaskBuilder extends AbstractFileTaskBuilder<StatTaskBuilder, StatTask> {
		private Optional<Path> remote = Optional.empty();
		
		private StatTaskBuilder() {
		}

		/**
		 * Create a new {@link StatTaskBuilder}

		 * @return builder
		 */
		public static StatTaskBuilder create() {
			return new StatTaskBuilder();
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemote(Optional<Path> remote) {
			this.remote = remote;
			return this;
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}
		
		@Override
		public StatTask build() {
			return new StatTask(this);
		}
	}

	private final Path remote;
	private SftpFileAttributes attrs = null;

	private StatTask(StatTaskBuilder builder) {
		super(builder);
		remote = builder.remote.orElseThrow(() -> new IllegalStateException("Remote path must be supplied."));
	}

	/**
	 * Construct a stat task. Deprecated since 3.1.0. Use a {@link StatTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param path path
	 * @deprecated 
	 * @see StatTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public StatTask(Connection<SshClientContext> con, String path) {
		this(StatTaskBuilder.create().withConnection(con).withRemotePath(path));
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> attrs = self.stat(remote.toString())));
	}

	public SftpFileAttributes getAttributes() {
		return attrs;
	}

}
