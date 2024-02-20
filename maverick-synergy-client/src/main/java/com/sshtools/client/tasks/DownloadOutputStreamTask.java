package com.sshtools.client.tasks;

import static com.sshtools.common.util.Utils.translatePathString;

import java.io.OutputStream;
import java.nio.file.Path;
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
 *      withRemotePath("/remote/to/remote.txt").
 *      build());
 * </pre>
 */
public class DownloadOutputStreamTask extends AbstractFileTask {

	/**
	 * Builder for {@link DownloadOutputStreamTask}.
	 */
	public final static class DownloadOutputStreamTaskBuilder extends AbstractFileTaskBuilder<DownloadOutputStreamTaskBuilder, DownloadOutputStreamTask> {
		private Optional<Path> remote = Optional.empty();
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
		 * Set the remote remote of the local to download.
		 * 
		 * @param remote remote remote
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote remote of the local to download.
		 * 
		 * @param remote remote remote
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote remote of the local to download.
		 * 
		 * @param remote remote  remote
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withRemote(Optional<Path> remote) {
			this.remote = remote;
			return this;
		}
		
		/**
		 * Set the remote remote of the local to download.
		 * 
		 * @param remote remote  remote
		 * @return builder for chaining
		 */
		public DownloadOutputStreamTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}

		@Override
		public DownloadOutputStreamTask build() {
			return new DownloadOutputStreamTask(this);
		}
	}

	final Path path;
	final OutputStream output;

	private DownloadOutputStreamTask(DownloadOutputStreamTaskBuilder builder) {
		super(builder);
		output = builder.outputStream.orElseThrow(() -> new IllegalStateException("OutputStream must be supplied."));
		path  = builder.remote.orElseThrow(() -> new IllegalStateException("Path must be supplied."));
	}

	/**
	 * Construct a new download file task. Deprecated since 3.1.0. Use a {@link DownloadOutputStreamTaskBuilder} instead. 
	 * 
	 * @param con connection
	 * @param remote remote
	 * @param output output
	 * @deprecated 
	 * @see DownloadFileTaskBuilder
	 */
	public DownloadOutputStreamTask(Connection<SshClientContext> con, String path, OutputStream output) {
		this(DownloadOutputStreamTaskBuilder.create().withConnection(con).withRemotePath(path).withOutputStream(output));
	}

	@Override
	protected void doTask() {
		try {
			doTaskUntilDone(new SftpClientTask(con, (self) -> self.get(translatePathString(path), output, progress.orElse(null))));
		} finally {
			IOUtils.closeStream(output);
		}
	}
}
