package com.sshtools.client.tasks;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.MultiIOException;
import com.sshtools.common.ssh.SshException;

public abstract class AbstractOptimisedTask<TARGET, LOCALFILE> extends AbstractFileTask {/**
	 * Interface to be implemented by classes that monitor the progress of file
	 * transfers and output feedback. <br/>
	 * Each file will have it's own instance of a {@link ProgressMessages}, as
	 * created by the {@link Function} supplied to
	 * {@link PushTaskBuilder#withProgressMessages(ProgressMessages)}.
	 */
	@FunctionalInterface
	public interface ProgressMessages {
		/**
		 * Display a message. See {@link MessageFormat} for expected format. If no
		 * arguments are supplied, the formatting pattern should be displayed as raw
		 * text.
		 * 
		 * @param fmt  formatting pattern.
		 * @param args arguments
		 */
		void message(String fmt, Object... args);

		/**
		 * Display an error.
		 * 
		 * @param exception error
		 */
		default void error(Throwable exception) {
			error(null, exception);
		}

		/**
		 * Display an (optional) error trace along with an (optional) message. If no
		 * arguments are supplied, the formatting pattern should be displayed as raw
		 * text.
		 * 
		 * @param fmt       formatting pattern.
		 * @param exception error
		 * @param args      arguments
		 */
		default void error(String fmt, Throwable exception, Object... args) {
			if (fmt != null) {
				message(fmt, args);
			}
			if (exception != null) {
				var sw = new StringWriter();
				exception.printStackTrace(new PrintWriter(sw, true));
				message(sw.toString());
			}
		}
	}
	
	public static abstract class AbstractOptimisedTaskBuilder<B extends AbstractOptimisedTaskBuilder<B, T, LOCALFILE>, T extends AbstractOptimisedTask<?, ?>, LOCALFILE> extends 
		AbstractFileTaskBuilder<B,T> {
		
		private int chunks = 3;
		private int blocksize = 32768;
		private int buffersize = 1024000;
		private int outstandingRequests = 64;
		private boolean verifyIntegrity;
		private RemoteHash digest = RemoteHash.md5;
		private boolean ignoreIntegrity;
		private Optional<SftpClient> primarySftpClient = Optional.empty();
		private Optional<ProgressMessages> progressMessages = Optional.empty();
		private Function<LOCALFILE, FileTransferProgress> chunkProgress = (f) -> null;
		private boolean verboseOutput = false;
		

		protected AbstractOptimisedTaskBuilder() {
		}

		/**
		 * Set the {@link ProgressMessages} callback to receive various progress
		 * <strong>message</strong>, it does not receive the actual amount of progress.
		 * See {@link
		 * 
		 * @param progressMessages progressMessages
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withProgressMessages(ProgressMessages progressMessages) {
			this.progressMessages = Optional.of(progressMessages);
			return (B) this;
		}

		/**
		 * Set a {@link Function} that can create {@link FileTransferProgress} instances
		 * given a {@link AbstractFile}. Each {@link FileTransferProgress} monitors a
		 * <strong>Chunk</strong>, which will either be a complete file, or a portion of
		 * it. So if the function is called multiple times with the same
		 * {@link AbstractFile}, each call should create a new instance per chunk. Once
		 * created, {@link FileTransferProgress#started(long, String)} may be called
		 * multiple times if multiple paths are being transferred.
		 * 
		 * @param chunkProgress chunk transfer progress monitor
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withChunkProgress(Function<LOCALFILE, FileTransferProgress> chunkProgress) {
			this.chunkProgress = chunkProgress;
			return (B) this;
		}

		/**
		 * Set an {@link SftpClient} to use instead of creating a new one. Note, this is
		 * only used for the primary SFTP connection, others will always be created
		 * internally for chunked transfers.
		 * 
		 * @param primarySftpClient SFTP client
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withPrimarySftpClient(SftpClient primarySftpClient) {
			this.primarySftpClient = Optional.of(primarySftpClient);
			return (B) this;
		}

		/**
		 * Set how many chunks to split any paths into for transfer.
		 * 
		 * @param chunks
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withChunks(int chunks) {
			this.chunks = chunks;
			return (B)this;
		}

		/**
		 * The size of the buffer used to pre-read the file during upload. Defaults to 1MB.
		 * @param buffersize
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public final B withBufferSize(int buffersize) {
			this.buffersize = buffersize;
			return (B)this;
		}
		
		/**
		 * The integrity of any paths transferred will be verified using the configured
		 * digest (see {@link #withDigest(RemoteHash)}. If verification fails, an
		 * exception will be thrown during transfer.
		 * 
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withVerifyIntegrity() {
			this.verifyIntegrity = true;
			return (B)this;
		}

		/**
		 * Set whether to verify the integrity of any paths transferred using the
		 * configured digest (see {@link #withDigest(RemoteHash)}. If verification
		 * fails, an exception will be thrown during transfer.
		 * 
		 * @param verifyIntegrity integrity verification
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withIntegrityVerification(boolean verifyIntegrity) {
			this.verifyIntegrity = verifyIntegrity;
			return (B)this;
		}

		/**
		 * Only warn about integrity checks, do not fail entirely.
		 * 
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withIgnoreIntegrity() {
			this.ignoreIntegrity = true;
			return (B)this;
		}

		/**
		 * Only warn about integrity checks, do not fail entirely.
		 * 
		 * @param ignoreIntegrity ignore integrity
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withIgnoreIntegrity(boolean ignoreIntegrity) {
			this.ignoreIntegrity = ignoreIntegrity;
			return (B)this;
		}

		/**
		 * The message digest algorithm to use for integrity checks (see
		 * {@link #withVerifyIntegrity()}).
		 * 
		 * @param digest digest
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withDigest(RemoteHash digest) {
			this.digest = digest;
			return (B)this;
		}
		
		/**
		 * The SFTP block size to use in SFTP operations. The default
		 * is 32k which is generally the optimal amount but can be changed
		 * for experimentation. The API will also optimise the block size and change
		 * your value at runtime if it detects it would cause SFTP packages to be 
		 * fragmented across multiple data packets causing performance issues. 
		 * 
		 * The minimum block size supported is 4096 bytes and the maximum
		 * size supported is 65535 bytes.
		 * @param blocksize
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withBlocksize(int blocksize) {
			this.blocksize = blocksize;
			return (B)this;
		}
		
		/**
		 * The number of SFTP messages to send asynchronously before checking 
		 * for server responses. Sending messages asynchronously increases
		 * performance and the API will generally configure this to it's own
		 * optimal setting based on remote window space and block size. Use
		 * this to experiment with various settings on your own network.
		 * @param outstandingRequest
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withAsyncRequests(int outstandingRequest) {
			this.outstandingRequests = outstandingRequest;
			return (B)this;
		}
		
		/**
		 * Output verbose information about the operation.
		 * @return builder for chaining
		 */
		public final B withVerboseOutput() {
			return withVerboseOutput(true);
		}
		
		/**
		 * Output verbose information about the operation.
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public final B withVerboseOutput(boolean verboseOutput) {
			this.verboseOutput = verboseOutput;
			return (B)this;
		}

	}


	protected final int chunks;
	protected final int blocksize;
	protected final int buffersize;
	protected final int outstandingRequests;
	protected final boolean verifyIntegrity;
	protected final RemoteHash digest;
	protected final boolean ignoreIntegrity;
	protected final SftpClient primarySftpClient;
	protected final Optional<ProgressMessages> progressMessages;
	protected final Function<LOCALFILE, FileTransferProgress> chunkProgress;
	protected final LinkedList<SshClient> clients = new LinkedList<>();
	protected final boolean verboseOutput;

	protected AbstractOptimisedTask(AbstractOptimisedTaskBuilder<?,?, LOCALFILE> builder) {
		super(builder);
		this.chunks = builder.chunks;
		this.verifyIntegrity = builder.verifyIntegrity;
		this.digest = builder.digest;
		this.ignoreIntegrity = builder.ignoreIntegrity;
		this.chunkProgress = builder.chunkProgress;
		this.progressMessages = builder.progressMessages;
		this.blocksize = builder.blocksize;
		this.buffersize = builder.buffersize;
		this.outstandingRequests = builder.outstandingRequests;
		this.verboseOutput = builder.verboseOutput;
		
		try {
			primarySftpClient = builder.primarySftpClient.orElse(SftpClientBuilder.create().withConnection(con).build());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SshException | PermissionDeniedException e) {
			throw new IllegalArgumentException("Failed to create SFTP client.", e);
		}
	}
	
	@Override
	public final void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			configureConnections();
			transferFiles(configureTargetFolder());
		}));
	}

	protected abstract void transferFiles(TARGET targetFolder) throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException, ChannelOpenException;
	
	protected final void displayMessage(String message, Object... args ) {
		progressMessages.ifPresent((p) -> p.message(message, args));
	}
	
	protected final  void verboseMessage(String message, Object... args) {
		if(verboseOutput) {
			displayMessage(message, args);
		}
	}

	protected final  void configureConnections() throws IOException, SshException {
		
		displayMessage("Creating {0} connections to {1}@{2}:{3,number,#}", chunks,
				con.getUsername(), con.getRemoteIPAddress(), con.getRemotePort());
		
		for (int i = 0; i < chunks; i++) {
			clients.add(clientSupplier.get().apply(i + 1));
		}

		verboseMessage("Created {0} connections to {1}@{2}:{3,number,#}", chunks,
				con.getUsername(), con.getRemoteIPAddress(), con.getRemotePort());
	}

	protected abstract TARGET configureTargetFolder()
			throws IOException, SshException, PermissionDeniedException, SftpStatusException;

	protected final void checkErrors(Collection<Throwable> errors) throws IOException, TransferCancelledException {
		if (errors.isEmpty()) {
			return;
		}
		
		var firstCancel = errors.stream().filter(e -> e instanceof TransferCancelledException).findFirst().orElse(null);
		errors.removeIf(e -> e instanceof TransferCancelledException && e != firstCancel);
		
		if (errors.size() == 1) {
			var err = errors.iterator().next();
			if(err instanceof UncheckedIOException) {
				throw (IOException) err.getCause();
			}
			else if (err instanceof IOException) {
				throw (IOException) err;
			} else if (err instanceof RuntimeException) {
				throw (RuntimeException) err;
			} else if(err instanceof TransferCancelledException) {
				throw (TransferCancelledException)err;
			} else {
				throw new IOException(MessageFormat.format("Transfer could not be completed. {0}",
						err.getMessage() == null ? "" : err.getMessage()), err);
			}
		} else {
			throw new MultiIOException("Transfer could not be completed due to at least 2 errors.", errors);
		}
	}

	protected final void printChunkMessages(long chunkLength) {
		for(int i = 0 ; i < chunks; i++) {
			var chunk = i + 1;
			var pointer = i * chunkLength;
			verboseMessage("Starting chunk {0} at position {1} with length of {2} bytes",
					chunk, pointer, chunkLength);
		}
	}

	protected final void verifyIntegrity(Path localPath, String remotePath)
			throws SshException, SftpStatusException, IOException, PermissionDeniedException {

		if (verifyIntegrity) {

			try {
				displayMessage("Verifying {0}", localPath.getFileName().toString());
				if (primarySftpClient.verifyFiles(localPath.toAbsolutePath().toString(), remotePath, digest)) {
					displayMessage("The integrity of {0} has been verified", localPath.getFileName().toString());
				} else {
					throw new IOException(
							String.format("The local and remote paths DO NOT match", localPath.getFileName().toString()));
				}
			} catch (SftpStatusException e) {
				if (e.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
					if (!ignoreIntegrity) {
						throw new IOException(
								String.format("The remote server does not support integrity verification"));
					}
					displayMessage("Ignoring that the remote server does not support integrity verification");
				} else {
					throw e;
				}
			}
		}

	}

	protected static class FileTransferProgressWrapper implements FileTransferProgress {
		private final FileTransferProgress delegate;
		private final AtomicLong total;
		private volatile long bytesSoFar;
		private final Optional<FileTransferProgress> overallProgress;

		protected FileTransferProgressWrapper(FileTransferProgress delegate, Optional<FileTransferProgress> overallProgress,
				AtomicLong total) {
			this.delegate = delegate;
			this.total = total;
			this.overallProgress = overallProgress;
		}

		@Override
		public void started(long bytesTotal, String remoteFile) {
			this.bytesSoFar = 0;
			if (this.delegate != null)
				this.delegate.started(bytesTotal, remoteFile);
		}

		@Override
		public boolean isCancelled() {
			if (this.delegate != null)
				return this.delegate.isCancelled();
			return overallProgress.isPresent() && overallProgress.get().isCancelled();
		}

		@Override
		public void progressed(long bytesSoFar) {
			var add = bytesSoFar - this.bytesSoFar;
			var t = total.addAndGet(add);
			this.bytesSoFar = bytesSoFar;
			if (this.delegate != null)
				this.delegate.progressed(bytesSoFar);

			if (overallProgress.isPresent()) {
				overallProgress.get().progressed(t);
			}
		}

		@Override
		public void completed() {
			if (this.delegate != null)
				this.delegate.completed();
		}

	}
	
}
