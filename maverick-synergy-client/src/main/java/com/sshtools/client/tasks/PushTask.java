/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sshtools.client.ChunkInputStream;
import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder.ProgressMessages;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.MultiIOException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.Utils;

/**
 * An SFTP {@link Task} that uploads complete paths in multiple chunks
 * concurrently. You cannot directly create a {@link PushTask}, instead use
 * {@link PushTaskBuilder}.
 * 
 * <pre>
 * client.addTask(PushTaskBuilder.create().withFilePaths("a/b/c.txt", "/d/e/f/g.txt")
 * 		.withRemoteFolder("/path/on/remote").withChunks(5).build());
 * </pre>
 *
 */
public final class PushTask extends AbstractFileTask {
	/**
	 * Builder for {@link PushTask}.
	 */
	public static class PushTaskBuilder extends AbstractFileTaskBuilder<PushTaskBuilder, PushTask> {

		/**
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

		private Optional<Path> remoteFolder = Optional.empty();
		private int chunks = 3;
		private int blocksize = 32768;
		private int buffersize = 1024000;
		private int outstandingRequests = 64;
		private boolean verifyIntegrity;
		private RemoteHash digest = RemoteHash.md5;
		private boolean ignoreIntegrity;
		private List<Path> paths = new ArrayList<>();
		private Optional<SftpClient> primarySftpClient = Optional.empty();
		private Optional<ProgressMessages> progressMessages = Optional.empty();
		private Function<AbstractFile, FileTransferProgress> chunkProgress = (f) -> null;
		private boolean verboseOutput = false;
		

		private PushTaskBuilder() {
		}

		/**
		 * Create a new {@link PushTaskBuilder}
		 * 
		 * @return builder
		 */
		public static PushTaskBuilder create() {
			return new PushTaskBuilder();
		}


		/**
		 * Set the {@link ProgressMessages} callback to receive various progress
		 * <strong>message</strong>, it does not receive the actual amount of progress.
		 * See {@link
		 * 
		 * @param progressMessages progressMessages
		 * @return builder for chaining
		 */
		public PushTaskBuilder withProgressMessages(ProgressMessages progressMessages) {
			this.progressMessages = Optional.of(progressMessages);
			return this;
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
		public PushTaskBuilder withChunkProgress(Function<AbstractFile, FileTransferProgress> chunkProgress) {
			this.chunkProgress = chunkProgress;
			return this;
		}

		/**
		 * Set an {@link SftpClient} to use instead of creating a new one. Note, this is
		 * only used for the primary SFTP connection, others will always be created
		 * internally for chunked transfers.
		 * 
		 * @param primarySftpClient SFTP client
		 * @return builder for chaining
		 */
		public PushTaskBuilder withPrimarySftpClient(SftpClient primarySftpClient) {
			this.primarySftpClient = Optional.of(primarySftpClient);
			return this;
		}

		/**
		 * Add a collection of paths to transfer. Each should be the path of the
		 * <code>Local</code> file, and will be resolved against the current virtual
		 * file system configured on the {@link SftpClient}.
		 * 
		 * @param filePaths file paths to add.
		 * @return builder for chaining
		 */
		public PushTaskBuilder addFilePaths(Collection<String> filePaths) {
			this.paths.addAll(filePaths.stream().map(Path::of).collect(Collectors.toList()));
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
		public PushTaskBuilder addPaths(Collection<Path> paths) {
			this.paths.addAll(paths);
			return this;
		}

		/**
		 * Add a collection of files to transfer. Each should be the path of the
		 * <code>Local</code> file, and will be resolved against the current virtual
		 * file system configured on the {@link SftpClient}.
		 * 
		 * @param files file paths to add.
		 * @return builder for chaining
		 */
		public PushTaskBuilder addFiles(Collection<File> files) {
			this.paths.addAll(files.stream().map(File::toPath).collect(Collectors.toList()));
			return this;
		}

		/**
		 * Set a collection of file paths to transfer. Any paths already added to this
		 * builder will be replaced. Each should be the path of the <code>Local</code>
		 * file, and will be resolved against the current virtual file system configured
		 * on the {@link SftpClient}.
		 * 
		 * @param paths all file paths to transfer.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withFilePaths(Collection<String> files) {
			this.paths.clear();
			return addFilePaths(files);
		}

		/**
		 * Set a collection of file paths to transfer. Any paths already added to this
		 * builder will be replaced. Each should be the path of the <code>Local</code>
		 * file, and will be resolved against the current virtual file system configured
		 * on the {@link SftpClient}.
		 * 
		 * @param paths all file paths to transfer.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withPaths(Collection<Path> paths) {
			this.paths.clear();
			return addPaths(paths);
		}

		/**
		 * Set an array of files to transfer. Any paths already added to this builder
		 * will be replaced. Each should be the path of the <code>Local</code> file, and
		 * will be resolved against the current virtual file system configured on the
		 * {@link SftpClient}.
		 * 
		 * @param files all file paths to transfer.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withFiles(File... files) {
			this.paths.clear();
			return addFiles(Arrays.asList(files));
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
		public PushTaskBuilder withPaths(Path... paths) {
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
		public PushTaskBuilder withFilesPaths(String... filePaths) {
			this.paths.clear();
			return addFilePaths(Arrays.asList(filePaths));
		}

		/**
		 * Set the remote folder where any transferred paths will be placed.
		 * 
		 * @param remoteFolder remote folder
		 * @return builder for chaining
		 */
		public PushTaskBuilder withRemoteFolder(String remoteFolder) {
			return withRemoteFolder(remoteFolder == null || remoteFolder.equals("") ? Optional.empty()
					: Optional.of(Path.of(remoteFolder)));
		}

		/**
		 * Set the remote folder where any transferred paths will be placed.
		 * 
		 * @param remoteFolder remote folder
		 * @return builder for chaining
		 */
		public PushTaskBuilder withRemoteFolder(Path remoteFolder) {
			return withRemoteFolder(Optional.of(remoteFolder));
		}

		/**
		 * Set the remote folder where any transferred paths will be placed. If this
		 * evaluates to {@link Optional#empty()}, then the default remote folder will be
		 * used (e.g. the users home directory).
		 * 
		 * @param remoteFolder remote folder
		 * @return builder for chaining
		 */
		public PushTaskBuilder withRemoteFolder(Optional<Path> remoteFolder) {
			this.remoteFolder = remoteFolder;
			return this;
		}

		/**
		 * Set how many chunks to split any paths into for transfer.
		 * 
		 * @param chunks
		 * @return builder for chaining
		 */
		public PushTaskBuilder withChunks(int chunks) {
			this.chunks = chunks;
			return this;
		}

		/**
		 * The size of the buffer used to pre-read the file during upload. Defaults to 1MB.
		 * @param buffersize
		 * @return
		 */
		public PushTaskBuilder withBufferSize(int buffersize) {
			this.buffersize = buffersize;
			return this;
		}
		
		/**
		 * The integrity of any paths transferred will be verified using the configured
		 * digest (see {@link #withDigest(RemoteHash)}. If verification fails, an
		 * exception will be thrown during transfer.
		 * 
		 * @return builder for chaining
		 */
		public PushTaskBuilder withVerifyIntegrity() {
			this.verifyIntegrity = true;
			return this;
		}

		/**
		 * Set whether to verify the integrity of any paths transferred using the
		 * configured digest (see {@link #withDigest(RemoteHash)}. If verification
		 * fails, an exception will be thrown during transfer.
		 * 
		 * @param verifyIntegrity integrity verification
		 * @return builder for chaining
		 */
		public PushTaskBuilder withIntegrityVerification(boolean verifyIntegrity) {
			this.verifyIntegrity = verifyIntegrity;
			return this;
		}

		/**
		 * Only warn about integrity checks, do not fail entirely.
		 * 
		 * @return builder for chaining
		 */
		public PushTaskBuilder withIgnoreIntegrity() {
			this.ignoreIntegrity = true;
			return this;
		}

		/**
		 * Only warn about integrity checks, do not fail entirely.
		 * 
		 * @param ignoreIntegrity ignore integrity
		 * @return builder for chaining
		 */
		public PushTaskBuilder withIgnoreIntegrity(boolean ignoreIntegrity) {
			this.ignoreIntegrity = ignoreIntegrity;
			return this;
		}

		/**
		 * The message digest algorithm to use for integrity checks (see
		 * {@link #withVerifyIntegrity()}).
		 * 
		 * @param digest digest
		 * @return builder for chaining
		 */
		public PushTaskBuilder withDigest(RemoteHash digest) {
			this.digest = digest;
			return this;
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
		public PushTaskBuilder withBlocksize(int blocksize) {
			this.blocksize = blocksize;
			return this;
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
		public PushTaskBuilder withAsyncRequests(int outstandingRequest) {
			this.outstandingRequests = outstandingRequest;
			return this;
		}
		
		/**
		 * Output verbose information about the operation.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withVerboseOutput() {
			return withVerboseOutput(true);
		}
		
		/**
		 * Output verbose information about the operation.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withVerboseOutput(boolean verboseOutput) {
			this.verboseOutput = verboseOutput;
			return this;
		}

		/**
		 * Build a new {@link PushTask} that may be scheduled for execution (e.g.
		 * {@link SshClient#addTask(Task)}). The created task takes a copy of the
		 * configuration in this builder for the immutable task, so if the builder is
		 * changed after building the task instance, it will not be affected.
		 * 
		 * @return task
		 */
		public PushTask build() {
			return new PushTask(this);
		}


	}

	private final int chunks;
	private final int blocksize;
	private final int buffersize;
	private final int outstandingRequests;
	private final boolean verifyIntegrity;
	private final RemoteHash digest;
	private final boolean ignoreIntegrity;
	private final List<Path> files;
	private final SftpClient primarySftpClient;
	private final Optional<ProgressMessages> progressMessages;
	private final Function<AbstractFile, FileTransferProgress> chunkProgress;
	private final LinkedList<SshClient> clients = new LinkedList<>();
	private final boolean verboseOutput;
	private String remoteFolder;
	private SftpFileAttributes remoteAttrs = null;

	PushTask(PushTaskBuilder builder) {
		super(builder);
		this.remoteFolder = builder.remoteFolder.map(Utils::translatePathString).orElse(null);
		this.chunks = builder.chunks;
		this.verifyIntegrity = builder.verifyIntegrity;
		this.digest = builder.digest;
		this.ignoreIntegrity = builder.ignoreIntegrity;
		this.chunkProgress = builder.chunkProgress;
		this.files = Collections.unmodifiableList(new ArrayList<Path>(builder.paths));
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
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			configureConnections();
			configureRemoteFolder();
			transferFiles();
		}));
	}

	private void transferFiles() throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException, ChannelOpenException {

		for (var file : files) {
			if(!Files.exists(file)) {
				throw new FileNotFoundException(String.format("%s does not exist", file.getFileName()));
			}
		}
		
		for (var file : files) {
			transferFile(file);
		}
	}
	
	private void displayMessage(String message, Object... args ) {
		progressMessages.ifPresent((p) -> p.message(message, args));
	}
	
	private void verboseMessage(String message, Object... args) {
		if(verboseOutput) {
			displayMessage(message, args);
		}
	}

	private void configureConnections() throws IOException, SshException {
		
		displayMessage("Creating {0} connections to {1}@{2}:{3,number,#}", chunks,
				con.getUsername(), con.getRemoteIPAddress(), con.getRemotePort());
		
		for (int i = 0; i < chunks; i++) {
			clients.add(clientSupplier.get().apply(i + 1));
		}

		verboseMessage("Created {0} connections to {1}@{2}:{3,number,#}", chunks,
				con.getUsername(), con.getRemoteIPAddress(), con.getRemotePort());
	}

	private void configureRemoteFolder()
			throws IOException, SshException, PermissionDeniedException, SftpStatusException {

		if (Utils.isNotBlank(remoteFolder)) {
			remoteFolder = primarySftpClient.getAbsolutePath(remoteFolder);
		} else {
			remoteFolder = primarySftpClient.getAbsolutePath(".");
		}

		remoteAttrs = primarySftpClient.stat(remoteFolder);
		if (!remoteAttrs.isDirectory()) {
			throw new IOException("Remote directory must be a directory!");
		}

		verboseMessage("The paths will be transferred to {0}", remoteFolder);
	}

	private void transferFile(Path file) throws SftpStatusException, SshException, TransferCancelledException,
			IOException, PermissionDeniedException, ChannelOpenException {

		var localFile = primarySftpClient.getCurrentWorkingDirectory().resolveFile(file.toString());
		verboseMessage("Total to transfer is {0} bytes", localFile.length());

		if (chunks <= 1) {
			sendFileViaSFTP(localFile, "");
			verifyIntegrity(localFile);
		} else {
			checkErrors(sendChunks(localFile));
			verifyIntegrity(localFile);
		}

	}

	private void checkErrors(Collection<Throwable> errors) throws IOException, TransferCancelledException {
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

	private Collection<Throwable> sendChunks(AbstractFile localFile)
			throws PermissionDeniedException, IOException, SftpStatusException, SshException {

		var executor = Executors.newFixedThreadPool(chunks);
		
		try {

			var chunkLength = localFile.length() /  chunks;
			var finalLength = localFile.length() - (chunkLength * (chunks-1));
			
			var progressChunks = Collections.synchronizedList(new ArrayList<FileTransferProgressWrapper>());
			var errors = Collections.synchronizedList(new ArrayList<Throwable>());
			var total = new AtomicLong();
			
			verboseMessage("Splitting {0} into {1} chunks", localFile.getName(), chunks);

			if (progress.isPresent()) {
				progress.get().started(localFile.length(), localFile.getName());
			}
			
			String remotePath = FileUtils.checkEndsWithSlash(primarySftpClient.pwd()) + localFile.getName();

			ByteArrayWriter msg = new ByteArrayWriter();
			msg.writeString(remotePath);
			msg.writeInt(chunks);
			for(int i = 0 ; i < chunks-1; i++) {
				var chunk = i + 1;
				msg.writeString(String.format("part%d", chunk));
				msg.writeUINT64(i * chunkLength);
				msg.writeUINT64(chunkLength);
			}
			
			msg.writeString(String.format("part%d", chunks));
			msg.writeUINT64((chunks-1) * chunkLength);
			msg.writeUINT64(finalLength);
		
			UnsignedInteger32 requestId;
			try {
				requestId = primarySftpClient.getSubsystemChannel().sendExtensionMessage("create-multipart-file@sshtools.com", msg.toByteArray());
			
				byte[] transaction = primarySftpClient.getSubsystemChannel().getHandleResponse(requestId);
				
				verboseMessage("Remote server supports multipart extensions");
				printChunkMessages(chunkLength);
				
				for (int i = 0; i < chunks; i++) {
					var chunk = i + 1;
					var pointer = i * chunkLength;
					executor.submit(() -> {
						try {
							var tmp = chunkProgress.apply(localFile);
							var wrapper = new FileTransferProgressWrapper(tmp, progress, total);
							progressChunks.add(wrapper);
							var lastChunk = chunk == chunks;
							var thisLength = lastChunk ? chunkLength + finalLength : chunkLength;
							
							sendPart(localFile, pointer, thisLength, chunk, lastChunk, wrapper, transaction, String.format("part%d", chunk));
							
						} catch (Throwable e) {
							errors.add(e);
						}
					});
					
				}
				
			} catch(SftpStatusException e) {
				/**
				 * Fallback to standard random access mode. Don't be so strict on the reason
				 * code because we don't know what other servers are sending back in response
				 * to an unknown extension.
				 */
				verboseMessage("Falling back to pure random access support which may or may not be supported.");
				printChunkMessages(chunkLength);
				for (int i = 0; i < chunks; i++) {
					var chunk = i + 1;
					var pointer = i * chunkLength;
					executor.submit(() -> {
						try {
							var tmp = chunkProgress.apply(localFile);
							var wrapper = new FileTransferProgressWrapper(tmp, progress, total);
							progressChunks.add(wrapper);
							var lastChunk = chunk == chunks;
							var thisLength = lastChunk ? chunkLength + finalLength : chunkLength;
							sendChunk(localFile, pointer, thisLength, chunk, lastChunk, wrapper);
						} catch (Throwable e2) {
							errors.add(e2);
						}
					});
				}
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

	private void printChunkMessages(long chunkLength) {
		for(int i = 0 ; i < chunks; i++) {
			var chunk = i + 1;
			var pointer = i * chunkLength;
			verboseMessage("Starting chunk {0} at position {1} with length of {2} bytes",
					chunk, pointer, chunkLength);
		}
	}

	private void sendFileViaSFTP(AbstractFile localFile, String remotePath) throws IOException, SshException,
			PermissionDeniedException, SftpStatusException, TransferCancelledException {
		var ssh = clients.removeFirst();
		var bldr = SftpClientBuilder.create().withClient(ssh);
		if(blocksize > 0) {
			bldr.withBlockSize(blocksize);
		}
		if(outstandingRequests > 0) {
			bldr.withAsyncRequests(outstandingRequests);
		}
		try (var sftp = bldr.build()) {
			sftp.lcd(primarySftpClient.getCurrentWorkingDirectory().getAbsolutePath());
			sftp.cd(remoteFolder);
			sftp.put(localFile.getAbsolutePath(), remotePath, progress.orElse(null));
		} finally {
			synchronized(clients) {
				clients.addLast(ssh);
			}
		}
	}

	private void verifyIntegrity(AbstractFile localFile)
			throws SshException, SftpStatusException, IOException, PermissionDeniedException {

		if (verifyIntegrity) {

			try {
				primarySftpClient.cd(remoteFolder);
				displayMessage("Verifying {0}", localFile.getName());
				if (primarySftpClient.verifyFiles(localFile.getAbsolutePath(), localFile.getName(), digest)) {
					displayMessage("The integrity of {0} has been verified", localFile.getName());
				} else {
					throw new IOException(
							String.format("The local and remote paths DO NOT match", localFile.getName()));
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

	private void sendChunk(AbstractFile localFile, long pointer, long chunkLength, Integer chunkNumber,
			boolean lastChunk, FileTransferProgress progress)
			throws IOException, SftpStatusException, SshException, TransferCancelledException, ChannelOpenException,
			PermissionDeniedException {

		verboseMessage("There are {0} clients, removing one", clients.size());
		SshClient ssh;
		synchronized(clients) {
			ssh = clients.removeFirst();
		}
		try (var file = new RandomAccessFile(localFile.getAbsolutePath(), "r")) {
			file.seek(pointer);
				try (var sftp = SftpClientBuilder.create().
						withClient(ssh).
						withRemotePath(remoteFolder).
						withLocalPath(primarySftpClient.lpwd()).
						build()) {
					
					try {
						sftp.put(new ChunkInputStream(file, chunkLength), localFile.getName(),
								new FileTransferProgress() {
	
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
	
								}, pointer, chunkLength);
					}
					catch(SftpStatusException e) {
						if(e.getStatus() == SftpStatusException.SSH_FX_NO_SUCH_FILE) {
							FileNotFoundException fnfe = new FileNotFoundException(localFile.getName() + " (chunk " + chunkNumber + " @ " + pointer + ", with " + chunkLength + " bytes)");
							fnfe.initCause(e);
							throw fnfe;
						}
						else
							throw e;
					}
				}
		} 
		catch(IOException ioe) {
			if(ioe.getCause() instanceof TransferCancelledException) {
				throw (TransferCancelledException)ioe.getCause();
			}
			else
				throw ioe;
		} finally {
			synchronized(clients) {
				clients.addLast(ssh);
			}
		}
	}
	
	private void sendPart(AbstractFile localFile, long pointer, long chunkLength, Integer chunkNumber,
			boolean lastChunk, FileTransferProgress progress, byte[] transaction, String partId)
			throws IOException, SftpStatusException, SshException, TransferCancelledException, ChannelOpenException,
			PermissionDeniedException {

		SshClient ssh;
		synchronized(clients) {
			ssh = clients.removeFirst();
		}
		try (var file = new RandomAccessFile(localFile.getAbsolutePath(), "r")) {
			file.seek(pointer);
				try (var sftp = SftpClientBuilder.create().
						withClient(ssh).
						withRemotePath(remoteFolder).
						withLocalPath(primarySftpClient.lpwd()).
						build()) {

					var msg = new ByteArrayWriter();
					msg.writeBinaryString(transaction);
					msg.writeString(partId);
					
					try(var handle = sftp.getSubsystemChannel().getHandle(sftp.getSubsystemChannel().sendExtensionMessage("open-part-file@sshtools.com", msg.toByteArray()))) {
						handle.performOptimizedWrite(localFile.getName(), 
								blocksize,
								outstandingRequests, 
								new ChunkInputStream(file, chunkLength),
									buffersize,
									new FileTransferProgress() {

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
		} 
		catch(IOException ioe) {
			if(ioe.getCause() instanceof TransferCancelledException) {
				throw (TransferCancelledException)ioe.getCause();
			}
			else
				throw ioe;
		} finally {
			synchronized(clients) {
				clients.addLast(ssh);
			}
		}
	}

	private static class FileTransferProgressWrapper implements FileTransferProgress {
		private final FileTransferProgress delegate;
		private final AtomicLong total;
		private volatile long bytesSoFar;
		private final Optional<FileTransferProgress> overallProgress;

		FileTransferProgressWrapper(FileTransferProgress delegate, Optional<FileTransferProgress> overallProgress,
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
