package com.sshtools.client.tasks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;

/**
 * An SFTP {@link Task} that downloads complete paths in multiple chunks
 * concurrently. You cannot directly create a {@link PullTask}, instead use
 * {@link PullTaskBuilder}.
 * 
 * <pre>
 * client.addTask(PullTaskBuilder.create().
 * 		withPaths("a/b/c.txt", "/d/e/f/g.txt").
 * 		withLocalFolder("/path/on/local").
 * 		withChunks(5).
 * 		build());
 * </pre>
 *
 */
public final class PullTask extends AbstractOptimisedTask<Path, String> {
	/**
	 * Builder for {@link PullTask}.
	 */
	public static class PullTaskBuilder extends AbstractOptimisedTaskBuilder<PullTaskBuilder, PullTask, String> {

		private Optional<Path> localFolder = Optional.empty();
		private List<String> paths = new ArrayList<>();

		private PullTaskBuilder() {
		}

		/**
		 * Create a new {@link PullTaskBuilder}
		 * 
		 * @return builder
		 */
		public static PullTaskBuilder create() {
			return new PullTaskBuilder();
		}

		/**
		 * Add a collection of paths to transfer. Each should be the path of the
		 * <code>Local</code> file, and will be resolved against the current virtual
		 * file system configured on the {@link SftpClient}.
		 * 
		 * @param filePaths file paths to add.
		 * @return builder for chaining
		 */
		public PullTaskBuilder addPaths(Collection<String> filePaths) {
			this.paths.addAll(filePaths);
			return this;
		}

		/**
		 * Add a collection of paths to transfer. Each should be the path of the
		 * <code>Local</code> file, and will be resolved against the current virtual
		 * file system configured on the {@link SftpClient}.
		 * 
		 * @param paths file paths to add.
		 * @return builder for chaining
		 */
		public PullTaskBuilder addPaths(String... paths) {
			return addPaths(Arrays.asList(paths));
		}

		/**
		 * Set an array of files to transfer. Any paths already added to this builder
		 * will be replaced. Each should be the path of the <code>Local</code> file, and
		 * will be resolved against the current virtual file system configured on the
		 * {@link SftpClient}.
		 * 
		 * @param paths all file paths to transfer.
		 * @return builder for chaining
		 */
		public PullTaskBuilder withPaths(String... paths) {
			this.paths.clear();
			return addPaths(Arrays.asList(paths));
		}

		/**
		 * Set an array of files to transfer. Any paths already added to this builder
		 * will be replaced. Each should be the path of the <code>Local</code> file, and
		 * will be resolved against the current virtual file system configured on the
		 * {@link SftpClient}.
		 * 
		 * @param filePaths all file paths to transfer.
		 * @return builder for chaining
		 */
		public PullTaskBuilder withPaths(Collection<String> filePaths) {
			this.paths.clear();
			return addPaths(filePaths);
		}

		/**
		 * Set the local folder where any transferred paths will be placed.
		 * 
		 * @param localFolder remote folder
		 * @return builder for chaining
		 */
		public PullTaskBuilder withLocalFolder(String localFolder) {
			return withLocalFolder(localFolder == null || localFolder.equals("") ? Optional.empty()
					: Optional.of(Path.of(localFolder)));
		}

		/**
		 * Set the remote folder where any transferred paths will be placed.
		 * 
		 * @param localFolder remote folder
		 * @return builder for chaining
		 */
		public PullTaskBuilder withLocalFolder(Path localFolder) {
			return withLocalFolder(Optional.of(localFolder));
		}

		/**
		 * Set the remote folder where any transferred paths will be placed. If this
		 * evaluates to {@link Optional#empty()}, then the default remote folder will be
		 * used (e.g. the users home directory).
		 * 
		 * @param localFolder local folder
		 * @return builder for chaining
		 */
		public PullTaskBuilder withLocalFolder(Optional<Path> localFolder) {
			this.localFolder = localFolder;
			return this;
		}

		/**
		 * Build a new {@link PullTask} that may be scheduled for execution (e.g.
		 * {@link SshClient#addTask(Task)}). The created task takes a copy of the
		 * configuration in this builder for the immutable task, so if the builder is
		 * changed after building the task instance, it will not be affected.
		 * 
		 * @return task
		 */
		public PullTask build() {
			return new PullTask(this);
		}

	}

	private final List<String> files;
	private Optional<Path> localFolder;

	PullTask(PullTaskBuilder builder) {
		super(builder);
		this.localFolder = builder.localFolder;
		this.files = Collections.unmodifiableList(builder.paths);
	}

	@Override
	protected void transferFiles(Path target) throws SftpStatusException, SshException, TransferCancelledException,
			IOException, PermissionDeniedException, ChannelOpenException {

		if (!Files.isDirectory(target)) {
			throw new IOException(MessageFormat.format("Local directory {0} must be a directory!", target));
		}

		verboseMessage("The paths will be transferred to {0}", target);

		for (var file : files) {
			if (!primarySftpClient.exists(file)) {
				throw new FileNotFoundException(String.format("%s does not exist", file));
			}
		}

		for (var file : files) {
			transferFile(file, target);
		}
	}

	@Override
	protected Path configureTargetFolder()
			throws IOException, SshException, PermissionDeniedException, SftpStatusException {
		return localFolder.orElseGet(() -> Paths.get(System.getProperty("user.dir")));
	}

	private void transferFile(String remotePath, Path localFolder) throws SftpStatusException, SshException,
			TransferCancelledException, IOException, PermissionDeniedException, ChannelOpenException {

		var remoteFile = primarySftpClient.stat(remotePath);
		verboseMessage("Total to transfer is {0} bytes", remoteFile.size());

		if (chunks <= 1) {
			receiveFileViaSFTP(remotePath, localFolder);
		} else {
			checkErrors(receiveChunks(remotePath, localFolder));
		}
		verifyIntegrity(localFolder.resolve(Paths.get(remotePath).getFileName()), remotePath);

	}

	private Collection<Throwable> receiveChunks(String remotePath, Path localFolder)
			throws PermissionDeniedException, IOException, SftpStatusException, SshException {

		var executor = Executors.newFixedThreadPool(chunks);
		var remoteFolder = primarySftpClient.pwd();

		try {
			var targetFilePath = localFolder.resolve(Paths.get(remotePath).getFileName());
			var remoteFile = primarySftpClient.stat(remotePath);

			var chunkLength = remoteFile.size().longValue() / chunks;
			var finalLength = remoteFile.size().longValue() - (chunkLength * (chunks - 1));

			var progressChunks = Collections.synchronizedList(new ArrayList<FileTransferProgressWrapper>());
			var errors = Collections.synchronizedList(new ArrayList<Throwable>());
			var total = new AtomicLong();

			verboseMessage("Splitting {0} into {1} chunks", targetFilePath.getFileName(), chunks);

			if (progress.isPresent()) {
				progress.get().started(remoteFile.size().longValue(), targetFilePath.getFileName().toString());
			}

			try (var localOutChannel = Files.newByteChannel(targetFilePath, StandardOpenOption.WRITE,
					StandardOpenOption.CREATE)) {
				localOutChannel.truncate(remoteFile.size().longValue());
			}

			verboseMessage("Remote server supports multipart extensions");
			printChunkMessages(chunkLength);

			for (int i = 0; i < chunks; i++) {
				var chunk = i + 1;
				var pointer = i * chunkLength;
				executor.submit(() -> {
					try {
						var tmp = chunkProgress.apply(remotePath);
						var wrapper = new FileTransferProgressWrapper(tmp, progress, total);
						progressChunks.add(wrapper);
						var lastChunk = chunk == chunks;
						var thisLength = lastChunk ? chunkLength + finalLength : chunkLength;

						receivePart(remotePath, pointer, (int)thisLength, chunk, lastChunk, wrapper,
								String.format("part%d", chunk), localFolder, remoteFile.size().longValue(), remoteFolder);

					} catch (Throwable e) {
						errors.add(e);
					}
				});

			}

			return errors;
		} finally {
			executor.shutdown();

			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e1) {
				throw new InterruptedIOException();
			} finally {
				progress.get().completed();
			}
		}
	}

	private void receiveFileViaSFTP(String remotePath, Path localFolder) throws IOException, SshException,
			PermissionDeniedException, SftpStatusException, TransferCancelledException {
		var ssh = clients.removeFirst();
		var bldr = SftpClientBuilder.create().withClient(ssh);
		if (blocksize > 0) {
			bldr.withBlockSize(blocksize);
		}
		if (outstandingRequests > 0) {
			bldr.withAsyncRequests(outstandingRequests);
		}
		try (var sftp = bldr.build()) {
			sftp.cd(primarySftpClient.pwd());
			sftp.lcd(localFolder.toAbsolutePath().toString());
			sftp.get(remotePath, progress.orElse(null));
		} finally {
			synchronized (clients) {
				clients.addLast(ssh);
			}
		}
	}

	private void receivePart(String remotePath, long pointer, int chunkLength, Integer chunkNumber, boolean lastChunk,
			FileTransferProgress progress, String partId, Path localFolder, long totalLength, String remoteFolder) throws IOException, SftpStatusException,
			SshException, TransferCancelledException, ChannelOpenException, PermissionDeniedException {

		SshClient ssh;
		synchronized (clients) {
			ssh = clients.removeFirst();
		}

		var targetFilePath = localFolder.resolve(Paths.get(remotePath).getFileName());
		try (var file = Files.newByteChannel(targetFilePath, StandardOpenOption.WRITE)) {
			file.position(pointer);
			try (var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath(remoteFolder).
					withLocalPath(localFolder.toAbsolutePath().toString()).build()) {

				try (var handle = sftp.getSubsystemChannel().openFile(remotePath, SftpChannel.OPEN_READ)) {
					handle.performOptimizedRead(totalLength, chunkLength, Channels.newOutputStream(file), outstandingRequests, new FileTransferProgress() {

						@Override
						public void started(long bytesTotal, String file) {
							progress.started(bytesTotal, file);
						}

						@Override
						public boolean isCancelled() {
							return progress.isCancelled();
						}

						@Override
						public void progressed(long bytesSoFar) {
							progress.progressed(bytesSoFar - pointer);
						}

						@Override
						public void completed() {
							progress.completed();
						}

					}, pointer);
					
					
				} catch (SftpStatusException | SshException | TransferCancelledException e) {
					Log.error("Part upload failed", e);
					throw e;
				}
			}
		} catch (IOException ioe) {
			if (ioe.getCause() instanceof TransferCancelledException) {
				throw (TransferCancelledException) ioe.getCause();
			} else
				throw ioe;
		} finally {
			synchronized (clients) {
				clients.addLast(ssh);
			}
		}
	}
}
