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

import java.io.InputStream;
import java.util.Optional;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClientTask;

/**
 * An SFTP {@link Task} that uploads complete files from an {@link InputStream}.
 * You cannot directly create a {@link UploadInputStreamTask}, instead use {@link UploadInputStreamTaskBuilder}.
 * <pre>
 * client.addTask(UploadInputStreamTaskBuilder.create().
 * 		withLocalFile(new File("local.txt")).
 *      withPath("/path/on/remote/remote.txt").
 *      build());
 * </pre>
 *
 */
public class UploadInputStreamTask extends AbstractFileTask {

	/**
	 * Builder for {@link UploadInputStreamTask}.
	 */
	public final static class UploadInputStreamTaskBuilder extends AbstractFileTaskBuilder<UploadInputStreamTaskBuilder, UploadInputStreamTask> {
		private Optional<String> path = Optional.empty();
		private Optional<InputStream> input = Optional.empty();
		private Optional<Long> length = Optional.empty();
		
		private UploadInputStreamTaskBuilder() {
		}

		/**
		 * Create a new {@link UploadInputStreamTaskBuilder}

		 * @return builder
		 */
		public static UploadInputStreamTaskBuilder create() {
			return new UploadInputStreamTaskBuilder();
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public UploadInputStreamTaskBuilder withPath(Optional<String> path) {
			this.path = path;
			return this;
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public UploadInputStreamTaskBuilder withPath(String path) {
			return withPath(Optional.of(path));
		}
		
		/**
		 * Set the {@link InputStream} to upload. This is required. If possible,
		 * you should also use {@link #withLength(long)} to set the content length.
		 * 
		 * @param input input stream
		 * @return builder for chaining
		 */
		public UploadInputStreamTaskBuilder withInputStream(InputStream input) {
			this.input = Optional.of(input);
			return this;
		}

		/**
		 * Set the length of content that will be uploaded (if known). This is
		 * only used by client code for progress calculation. It will be passed to 
		 * {@link FileTransferProgress#started(long, String)}. If not set, <code>-1</code>
		 * will be passed instead (meaning "indeterminate").
		 * 
		 * @param length length
		 * @return builder for chaining
		 */
		public UploadInputStreamTaskBuilder withLength(long length) {
			this.length = Optional.of(length);
			return this;
		}

		@Override
		public UploadInputStreamTask build() {
			return new UploadInputStreamTask(this);
		}
	}

	final String path;
	final InputStream input;
	final long length;

	private UploadInputStreamTask(UploadInputStreamTaskBuilder builder) {
		super(builder);
		path = builder.path.orElseThrow(() -> new IllegalStateException("Remote path must be supplied."));
		input = builder.input.orElseThrow(() -> new IllegalStateException("InputStream must be supplied."));
		length = builder.length.orElse(-1l);
	}

	/**
	 * Construct a new upload file task. Deprecated since 3.1.0. Use a {@link UploadInputStreamTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param localFile local file
	 * @param path path
	 * @deprecated 
	 * @see UploadInputStreamTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public UploadInputStreamTask(SshClient ssh, InputStream input, String path) {
		this(UploadInputStreamTaskBuilder.create().withClient(ssh).withPath(path).withInputStream(input));
	}
	
	@Override
	protected void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> self.put(input, path, progress.orElse(null), 0, length)));
	}
}
