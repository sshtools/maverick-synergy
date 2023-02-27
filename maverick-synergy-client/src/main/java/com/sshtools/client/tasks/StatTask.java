/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
