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

import static com.sshtools.common.util.Utils.translatePathString;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.synergy.ssh.Connection;

/**
 * An SFTP {@link Task} that uploads complete files.
 * You cannot directly create a {@link UploadFileTask}, instead use {@link UploadFileTaskBuilder}.
 * <pre>
 * client.addTask(StatTaskBuilder.create().
 * 		withLocalFile(new File("local.txt")).
 *      withRemotePath("/path/on/remote/remote.txt").
 *      build());
 * </pre>
 *
 */
public class UploadFileTask extends AbstractFileTask {

	/**
	 * Builder for {@link UploadFileTask}.
	 */
	public final static class UploadFileTaskBuilder extends AbstractFileTaskBuilder<UploadFileTaskBuilder, UploadFileTask> {
		private Optional<Path> path = Optional.empty();
		private Optional<Path> local = Optional.empty();
		
		private UploadFileTaskBuilder() {
		}

		/**
		 * Create a new {@link UploadFileTaskBuilder}

		 * @return builder
		 */
		public static UploadFileTaskBuilder create() {
			return new UploadFileTaskBuilder();
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remmote remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemote(Optional<Path> remmote) {
			this.path = remmote;
			return this;
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}
		
		/**
		 * Set the local file to upload. This is required.
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withLocalFile(File file) {
			return withLocal(file.toPath());
		}
		
		/**
		 * Set the local file to upload. This is required.
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withLocal(Path path) {
			this.local = Optional.of(path);
			return this;
		}

		@Override
		public UploadFileTask build() {
			return new UploadFileTask(this);
		}
	}

	final Optional<Path> remote;
	final Path local;

	private UploadFileTask(UploadFileTaskBuilder builder) {
		super(builder);
		remote = builder.path;
		local = builder.local.orElseThrow(() -> new IllegalStateException("Local file must be supplied."));
	}

	/**
	 * Construct a new upload file task. Deprecated since 3.1.0. Use a {@link UploadFileTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param localFile local file
	 * @param path path
	 * @deprecated 
	 * @see UploadFileTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public UploadFileTask(Connection<SshClientContext> con, File localFile, String path) {
		this(UploadFileTaskBuilder.create().withConnection(con).withRemotePath(path).withLocalFile(localFile));
	}

	/**
	 * Construct a new upload file task. Deprecated since 3.1.0. Use a {@link UploadFileTaskBuilder} instead. 
	 * 
	 * @param con
	 * @param localFile
	 * @deprecated 
	 * @see UploadFileTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public UploadFileTask(Connection<SshClientContext> con, File localFile) {
		this(con, localFile, null);
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			if(remote.isEmpty()) {
				self.put(local.toAbsolutePath().toString(), progress.orElse(null));
			} else {
				self.put(local.toAbsolutePath().toString(), translatePathString(remote.get()), progress.orElse(null));
			}
		}));
	}
}
