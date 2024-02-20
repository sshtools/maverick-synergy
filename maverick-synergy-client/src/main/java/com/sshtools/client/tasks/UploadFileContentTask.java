package com.sshtools.client.tasks;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.tasks.UploadInputStreamTask.UploadInputStreamTaskBuilder;
import com.sshtools.synergy.ssh.Connection;

/**
 * An SFTP {@link Task} that uploads string content to a remote file.
 * You cannot directly create a {@link UploadFileContentTask}, instead use {@link UploadInputStreamTaskBuilder}.
 * <pre>
 * client.addTask(UploadFileContentTask.create().
 * 		withContent("Hello World!").
 *      withRemotePath("/remote/on/remote/remote.txt").
 *      build());
 * </pre>
 *
 */
public class UploadFileContentTask extends AbstractFileTask {

	/**
	 * Builder for {@link UploadFileContentTask}.
	 */
	public final static class UploadFileContentTaskBuilder extends AbstractFileTaskBuilder<UploadFileContentTaskBuilder, UploadFileContentTask> {
		private Optional<Path> remote = Optional.empty();
		private Optional<Object> content = Optional.empty();
		private Optional<Charset> encoding = Optional.empty();
		
		private UploadFileContentTaskBuilder() {
		}

		/**
		 * Create a new {@link UploadFileContentTaskBuilder}

		 * @return builder
		 */
		public static UploadFileContentTaskBuilder create() {
			return new UploadFileContentTaskBuilder();
		}
		
		/** 
		 * Set the character encoding to use for transferring the string content.
		 * 
		 * @param encoding encoding 
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withEncoding(String encoding) {
			if(encoding == null) {
				this.encoding = Optional.empty();
				return this;
			}
			return withEncoding(Charset.forName(encoding));
		}
		
		/** 
		 * Set the character encoding to use for transferring the string content.
		 * 
		 * @param encoding encoding 
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withEncoding(Charset encoding) {
			this.encoding = Optional.of(encoding);
			return this;
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote  path
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withRemote(Optional<Path> remote) {
			this.remote = remote;
			return this;
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote  path
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}
		
		/**
		 * Set the {@link Object} to upload. The object will simple be deserialized using
		 * {@link Object#toString()}. This is required. 
		 * 
		 * @param content content
		 * @return builder for chaining
		 */
		public UploadFileContentTaskBuilder withContent(Object content) {
			this.content = Optional.of(content);
			return this;
		}

		@Override
		public UploadFileContentTask build() {
			return new UploadFileContentTask(this);
		}
	}

	final Path remote;
	final String content;
	final Charset encoding;

	private UploadFileContentTask(UploadFileContentTaskBuilder builder) {
		super(builder);
		remote = builder.remote.orElseThrow(() -> new IllegalStateException("Remote remote must be supplied."));
		content = String.valueOf(builder.content.orElseThrow(() -> new IllegalStateException("Content must be supplied.")));
		encoding = builder.encoding.orElse(Charset.defaultCharset());
	}

	/**
	 * Construct a new upload content task. Deprecated since 3.1.0. Use a {@link UploadFileContentTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param localFile local file
	 * @param remote remote
	 * @deprecated 
	 * @see UploadFileContentTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public UploadFileContentTask(SshClient ssh, String content, String encoding, String path) {
		this(ssh.getConnection(), content, encoding, path);
	}


	/**
	 * Construct a new upload content task. Deprecated since 3.1.0. Use a {@link UploadFileContentTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param content content
	 * @param encoding encoding
	 * @param remote remote
	 * @deprecated 
	 * @see UploadFileContentTaskBuilder
	 */
	@Deprecated(forRemoval = true, since = "3.1.0")
	public UploadFileContentTask(Connection<SshClientContext> con, String content, String encoding, String path) {
		this(UploadFileContentTaskBuilder.create().withConnection(con).withContent(content).withEncoding(encoding).withRemotePath(path));
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			var bytes = content.getBytes(encoding);
			self.put(new ByteArrayInputStream(bytes), remote.toString(), progress.orElse(null), 0, bytes.length);
		}));
	}
}
