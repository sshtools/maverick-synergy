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

import java.io.OutputStream;
import java.util.Optional;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.tasks.DownloadFileTask.DownloadFileTaskBuilder;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.ssh.Connection;

 /**
 * An SFTP {@link Task} that downloads complete files to an {@link OutputStream}.
 * You cannot directly create a {@link DownloadOutputStreamTask}, instead use {@link DownloadOutputStreamTas}.
 * <pre>
 * client.addTask(DownloadFileTaskBuilder.create().
 * 		withLocalFile(new File("local.txt")).
 *      withRemotePath("/path/to/remote.txt").
 *      build());
 * </pre>
 */
public class DownloadOutputStreamTask extends AbstractFileTask {

	/**
	 * Builder for {@link DownloadOutputStreamTask}.
	 */
	public final static class DownloadOutputStreamTaskBuilder extends AbstractFileTaskBuilder<DownloadOutputStreamTaskBuilder, DownloadOutputStreamTask> {
		private Optional<String> path = Optional.empty();
		private Optional<OutputStream> outputStream = Optional.empty();
		
		private DownloadOutputStreamTaskBuilder() {
		}

		/**
		 * Create a new {@link DownloadOutputStreamTaskBuilder}

		 * @return builder
		 */
		public static DownloadOutputStreamTaskBuilder create() {
			return new DownloadOutputStreamTaskBuilder();
		}
		
		/**
		 * Set the output stream to download to. If empty, will be download
		 * the current local working directory, with the same name as the remote file.
		 * 
		 * @param file file
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withOutputStream(Optional<OutputStream> outputStream) {
			this.outputStream = outputStream;
			return this;
		}
		
		/**
		 * Set the output stream to download to.
		 * 
		 * @param file file
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withOutputStream(OutputStream outputStream) {
			return withOutputStream(Optional.of(outputStream));
		}
		
		/**
		 * Set the remote path of the file to download.
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withPath(String path) {
			this.path = Optional.of(path);
			return this;
		}

		@Override
		public DownloadOutputStreamTask build() {
			return new DownloadOutputStreamTask(this);
		}
	}

	final String path;
	final OutputStream output;

	private DownloadOutputStreamTask(DownloadOutputStreamTaskBuilder builder) {
		super(builder);
		output = builder.outputStream.orElseThrow(() -> new IllegalStateException("OutputStream must be supplied."));
		path  = builder.path.orElseThrow(() -> new IllegalStateException("Path must be supplied."));
	}

	/**
	 * Construct a new download file task. Deprecated since 3.1.0. Use a {@link DownloadOutputStreamTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param path path
	 * @param output output
	 * @deprecated 
	 * @see DownloadFileTaskBuilder
	 */
	public DownloadOutputStreamTask(Connection<SshClientContext> con, String path, OutputStream output) {
		this(DownloadOutputStreamTaskBuilder.create().withConnection(con).withPath(path).withOutputStream(output));
	}

	@Override
	protected void doTask() {
		try {
			doTaskUntilDone(new SftpClientTask(con, (self) -> self.get(path, output, progress.orElse(null))));
		} finally {
			IOUtils.closeStream(output);
		}
	}
}
