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

import com.sshtools.client.sftp.SftpClientTask;

/**
 * An SFTP {@link Task} that downloads complete files.
 * You cannot directly create a {@link DownloadFileTask}, instead use {@link DownloadFileTaskBuilder}.
 * <pre>
 * client.addTask(DownloadFileTaskBuilder.create().
 * 		withLocalFile(new File("local.txt")).
 *      withRemotePath("/path/to/remote.txt").
 *      build());
 * </pre>
 */
public class DownloadFileTask extends AbstractFileTask {

	/**
	 * Builder for {@link DownloadFileTask}.
	 */
	public final static class DownloadFileTaskBuilder extends AbstractFileTaskBuilder<DownloadFileTaskBuilder, DownloadFileTask> {
		private Optional<Path> remote = Optional.empty();
		private Optional<Path> local = Optional.empty();
		
		private DownloadFileTaskBuilder() {
		}

		/**
		 * Create a new {@link DownloadFileTaskBuilder}

		 * @return builder
		 */
		public static DownloadFileTaskBuilder create() {
			return new DownloadFileTaskBuilder();
		}
		
		/**
		 * Set the local local to download to. If empty, will be download
		 * the current local working directory, with the same name as the remote local.
		 * 
		 * @param local local
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withLocalFile(Optional<File> file) {
			return withLocal(file.map(File::toPath));
		}
		
		/**
		 * Set the local local to download to. If empty, will be download
		 * the current local working directory, with the same name as the remote local.
		 * 
		 * @param local local
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withLocal(Optional<Path> file) {
			this.local = file;
			return this;
		}
		
		/**
		 * Set the local local to download to.
		 * 
		 * @param local local
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withLocalFile(File file) {
			return withLocal(file.toPath());
		}
		
		/**
		 * Set the local local to download to. If empty, will be download
		 * the current local working directory, with the same name as the remote local.
		 * 
		 * @param local local
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withLocal(Path file) {
			return withLocal(Optional.of(file));
		}
		
		/**
		 * Set the remote path of the local to download.
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote path of the local to download.
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote path of the local to download.
		 * 
		 * @param remote remote  path
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withRemote(Optional<Path> remote) {
			this.remote = remote;
			return this;
		}
		
		/**
		 * Set the remote path of the local to download.
		 * 
		 * @param remote remote  path
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}

		@Override
		public DownloadFileTask build() {
			return new DownloadFileTask(this);
		}
	}

	final Path remote;
	final Optional<Path> local;

	private DownloadFileTask(DownloadFileTaskBuilder builder) {
		super(builder);
		remote = builder.remote.orElseThrow(() -> new IllegalStateException("Remote path must be supplied."));
		local = builder.local;
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			self.get(translatePathString(remote), local.orElse(Path.of(self.lpwd())).toAbsolutePath().toString(), progress.orElse(null));
		}));
	}

	public File getDownloadedFile() {
		return local.orElseThrow().toFile();
	}

}
