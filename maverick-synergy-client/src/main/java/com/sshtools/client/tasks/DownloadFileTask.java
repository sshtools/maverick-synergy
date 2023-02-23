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

import java.io.File;
import java.util.Optional;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.common.util.FileUtils;
import com.sshtools.synergy.ssh.Connection;

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
		private Optional<String> path = Optional.empty();
		private Optional<File> file = Optional.empty();
		
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
		 * Set the local file to download to. If empty, will be download
		 * the current local working directory, with the same name as the remote file.
		 * 
		 * @param file file
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withLocalFile(Optional<File> file) {
			this.file = file;
			return this;
		}
		
		/**
		 * Set the local file to download to.
		 * 
		 * @param file file
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withLocalFile(File file) {
			return withLocalFile(Optional.of(file));
		}
		
		/**
		 * Set the remote path of the file to download.
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public DownloadFileTaskBuilder withPath(String path) {
			this.path = Optional.of(path);
			return this;
		}

		@Override
		public DownloadFileTask build() {
			return new DownloadFileTask(this);
		}
	}

	final String path;
	final Optional<File> localFile;

	private DownloadFileTask(DownloadFileTaskBuilder builder) {
		super(builder);
		path = builder.path.orElseThrow(() -> new IllegalStateException("Remote path must be supplied."));
		localFile = builder.file;
	}

	/**
	 * Construct a new download file task. Deprecated since 3.1.0. Use a {@link DownloadFileTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param path path
	 * @param localFile local file
	 * @deprecated 
	 * @see DownloadFileTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public DownloadFileTask(Connection<SshClientContext> con, String path, File localFile) {
		this(DownloadFileTaskBuilder.create().withConnection(con).withPath(path).withLocalFile(localFile));
	}

	/**
	 * Construct a new download file task. Deprecated since 3.1.0. Use a {@link DownloadFileTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param path path
	 * @deprecated 
	 * @see DownloadFileTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public DownloadFileTask(Connection<SshClientContext> con, String path) {
		this(con, path, null);
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			self.get(path, localFile.orElse(new File(self.lpwd(), FileUtils.getFilename(path))).getAbsolutePath(), progress.orElse(null));
		}));
	}

	public File getDownloadedFile() {
		return localFile.orElseThrow();
	}

}
