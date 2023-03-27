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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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
import com.sshtools.client.scp.ScpClient;
import com.sshtools.client.scp.ScpClientIO;
import com.sshtools.client.sftp.RemoteHash;
import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.client.shell.ExpectShell;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder.ProgressMessages;
import com.sshtools.client.tasks.ShellTask.ShellTaskBuilder;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.MultiIOException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger64;
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
		private int outstandingRequests = 64;
		private boolean verifyIntegrity;
		private RemoteHash digest = RemoteHash.md5;
		private boolean ignoreIntegrity;
		private boolean forceSFTP;
		private List<Path> paths = new ArrayList<>();
		private Optional<SftpClient> primarySftpClient = Optional.empty();
		private Optional<ProgressMessages> progressMessages = Optional.empty();
		private Function<AbstractFile, FileTransferProgress> chunkProgress = (f) -> null;
		public boolean ignoreCopyDataExtension;
		public boolean preAllocation = true;
		public boolean verboseOutput = false;
		

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
		 * Set to not pre-allocate a file of the required size. Usually, an attempt will
		 * be made to do so (currently using <code>fallocate</code> where available), in
		 * order to avoid having to recombine the chunks when transfer is complete.
		 * 
		 * @param ignoreCopyDataExtension ignore copy data extension
		 * @return builder for chaining
		 */
		public PushTaskBuilder withoutPreAllocation() {
			preAllocation = false;
			return this;
		}

		/**
		 * Set whether to pre-allocate a file to the required size. Usually, an attempt
		 * will be made to do so (currently using <code>fallocate</code> where
		 * available), in order to avoid having to recombine the chunks when transfer is
		 * complete.
		 * 
		 * @param ignoreCopyDataExtension ignore copy data extension
		 * @return builder for chaining
		 */
		public PushTaskBuilder withPreAllocation(boolean preAllocation) {
			this.preAllocation = preAllocation;
			return this;
		}

		/**
		 * Set to ignore the <code>copy-data</code> extension if it is supported,
		 * forcing a fallback to using shell command concatention (e.g. <code>cat</code>
		 * command) to recombine any chunks on the server.
		 * 
		 * @param ignoreCopyDataExtension ignore copy data extension
		 * @return builder for chaining
		 */
		public PushTaskBuilder withoutCopyDataExtension() {
			return withIgnoreCopyDataExtension(true);
		}

		/**
		 * Set whether to ignore the <code>copy-data</code> extension if it is
		 * supported, forcing a fallback to using shell command concatention (e.g.
		 * <code>cat</code> command) to recombine any chunks on the server.
		 * 
		 * @param ignoreCopyDataExtension ignore copy data extension
		 * @return builder for chaining
		 */
		public PushTaskBuilder withIgnoreCopyDataExtension(boolean ignoreCopyDataExtension) {
			this.ignoreCopyDataExtension = ignoreCopyDataExtension;
			return this;
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
		 * Force the use of SFTP, even when SCP would otherwise be chosen.
		 * 
		 * @return builder for chaining
		 */
		public PushTaskBuilder withForceSFTP() {
			this.forceSFTP = true;
			return this;
		}

		/**
		 * Set whether to force the use of SFTP, even when SCP would otherwise be
		 * chosen.
		 * 
		 * @return builder for chaining
		 */
		public PushTaskBuilder withSFTPForcing(boolean forceSFTP) {
			this.forceSFTP = forceSFTP;
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
		 * @return
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
		 * @return
		 */
		public PushTaskBuilder withAsyncRequests(int outstandingRequest) {
			this.outstandingRequests = outstandingRequest;
			return this;
		}
		
		/**
		 * Output verbose information about the operation.
		 * @return
		 */
		public PushTaskBuilder withVerboseOutput() {
			this.verboseOutput = true;
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
	private final int outstandingRequests;
	private final boolean verifyIntegrity;
	private final RemoteHash digest;
	private final boolean ignoreIntegrity;
	private final boolean forceSFTP;
	private final boolean preAllocation;
	private final boolean ignoreCopyDataExtension;
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
		this.remoteFolder = builder.remoteFolder.map(p -> p.toString()).orElse(null);
		this.chunks = builder.chunks;
		this.preAllocation = builder.preAllocation;
		this.verifyIntegrity = builder.verifyIntegrity;
		this.digest = builder.digest;
		this.ignoreIntegrity = builder.ignoreIntegrity;
		this.forceSFTP = builder.forceSFTP;
		this.chunkProgress = builder.chunkProgress;
		this.ignoreCopyDataExtension = builder.ignoreCopyDataExtension;
		this.files = Collections.unmodifiableList(new ArrayList<Path>(builder.paths));
		this.progressMessages = builder.progressMessages;
		this.blocksize = builder.blocksize;
		this.outstandingRequests = builder.outstandingRequests;
		this.verboseOutput = builder.verboseOutput;
		
		try {
			primarySftpClient = builder.primarySftpClient.orElse(new SftpClient(con));
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
		
		verboseMessage("Creating {0} connections to {1}@{2}:{3}", chunks,
				con.getUsername(), con.getRemoteIPAddress(), con.getRemotePort());
		
		for (int i = 0; i < chunks; i++) {
			clients.add(clientSupplier.get().apply(i + 1));
		}
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

		if (!localFile.exists()) {
			throw new IOException(String.format("%s does not exist!", localFile.getName()));
		}

		verboseMessage("Total to transfer is {0} bytes", localFile.length());

		if (chunks <= 1) {
			if (forceSFTP) {
				sendFileViaSFTP(localFile, "");
			} else {
				sendFileViaSCP(localFile, FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName());
			}

			verifyIntegrity(localFile);
		} else {
			var preAllocated = tryToAllocate(localFile);
			checkErrors(sendChunks(localFile, preAllocated));

			if (!preAllocated)
				combineChunks(localFile);

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

	private boolean tryToAllocate(AbstractFile localFile) throws IOException {
		if (!preAllocation) {
			displayMessage("Explicitly not pre-allocating file, total transfer operation may be slower.");
			return false;
		}
		var client = clientSupplier.orElseThrow(() -> new IllegalArgumentException("No connection or client supplied."))
				.apply(0);
		try {
			client.runTask(ShellTaskBuilder.create().
					withClient(client).
					onOpen((task, session) -> {
						Thread.sleep(1000);
						verboseMessage("Opening shell");
						
						var shell = new ExpectShell(task);
						execute(String.format("cd '%s'", escapeSingleQuotes(remoteFolder)), shell);
						
						verboseMessage("Allocating space");
						
						execute(String.format("fallocate -l %d '%s'", localFile.length(),
								escapeSingleQuotes(localFile.getName())), shell);
						
						verboseMessage("Allocated space");
					}).build());
			return true;
		} catch (Exception ioe) {
			displayMessage("Unable to pre-allocate space, will recombine chunks at end of transfer");
			return false;
		}
	}

	private void combineChunks(AbstractFile localFile) throws IOException, TransferCancelledException {

		verboseMessage("Combining parts into final file on remote machine");

		if (!performCopyData(localFile)) {
			performRemoteCat(localFile);
		}

	}

	private void performRemoteCat(AbstractFile localFile) throws IOException {
		var client = clientSupplier.orElseThrow(() -> new IllegalArgumentException("No connection or client supplied."))
				.apply(0);
		client.runTask(ShellTaskBuilder.create().withClient(client).onOpen((task, session) -> {
			verboseMessage("Opening shell");
			var shell = new ExpectShell(task);
			execute(String.format("cd '%s'", escapeSingleQuotes(remoteFolder)), shell);
			verboseMessage("Combining paths");
			execute(String.format("cat '%s_'* > '%s'", escapeSingleQuotes(localFile.getName()),
					escapeSingleQuotes(localFile.getName())), shell);
			verboseMessage("Deleting parts");
			execute(String.format("rm -f '%s_'*", escapeSingleQuotes(localFile.getName())), shell);
		}).build());
	}

	private String escapeSingleQuotes(String text) {
		return text.replace("'", "\\'");
	}

	private void execute(String command, ExpectShell shell) throws IOException, SshException {
		var process = shell.executeCommand(command);
		process.drain();
		if (process.hasSucceeded()) {
			if (Utils.isNotBlank(process.getCommandOutput())) {
				verboseMessage(process.getCommandOutput());
			}
		} else {
			if (Utils.isNotBlank(process.getCommandOutput())) {
				progressMessages.ifPresent((p) -> p.error(process.getCommandOutput(), null));
			}
			throw new IOException("Command failed");
		}
	}

	private boolean performCopyData(AbstractFile localFile) throws IOException, TransferCancelledException {

		var errors = new ArrayList<Throwable>();

		try {
			if (primarySftpClient.getSubsystemChannel().supportsExtension("copy-data")) {
				displayMessage("Remote server does not support copy-data SFTP extension");
				return false;
			}
			if (ignoreCopyDataExtension) {
				displayMessage("Remote server supports copy-data SFTP extension, but we are explicitly ignoring it.");
				return false;
			}
			primarySftpClient.cd(remoteFolder);
			var zero = new UnsignedInteger64(0);

			var remoteFile = primarySftpClient.getSubsystemChannel().openFile(
					FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName(),
					SftpChannel.OPEN_WRITE | SftpChannel.OPEN_CREATE | SftpChannel.OPEN_TRUNCATE);

			long position = 0L;
			var remoteChunks = new ArrayList<SftpFile>();

			try {
				for (int chunk = 1; chunk <= chunks; chunk++) {

					var remoteChunk = primarySftpClient.getSubsystemChannel().openFile(
							FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName() + "_part" + chunk,
							SftpChannel.OPEN_READ);
					remoteChunks.add(remoteChunk);
					try {
						primarySftpClient.copyRemoteData(remoteChunk, zero, zero, remoteFile,
								new UnsignedInteger64(position));
						verboseMessage("Copied part {0} of {1} to {2}", chunk, chunks, localFile.getName());
						position += remoteChunk.getAttributes().getSize().longValue();
					} catch (SftpStatusException e) {
						if (e.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
							displayMessage("Remote server does not support copy-data SFTP extension");
							return false;
						} else {
							errors.add(e);
						}
					} finally {
						remoteChunk.close();
					}
				}
			} finally {
				remoteFile.close();
			}

			checkErrors(removeChunks(localFile, remoteChunks));
		} catch (Throwable e) {
			errors.add(e);
		}

		checkErrors(errors);

		return true;
	}

	private Collection<Throwable> removeChunks(AbstractFile localFile, Collection<SftpFile> remoteChunks)
			throws IOException {

		var errors = new ArrayList<Throwable>();

		try {
			for (var remoteChunk : remoteChunks) {
				try {
					remoteChunk.delete();
				} catch (SftpStatusException | SshException e) {
					errors.add(e);
				}
			}
		} catch (Throwable e) {
			errors.add(e);
		}
		return errors;
	}

	private Collection<Throwable> sendChunks(AbstractFile localFile, boolean preallocated)
			throws PermissionDeniedException, IOException {

		verboseMessage("Splitting {0} into {1} chunks", localFile.getName(), chunks);

		var executor = Executors.newFixedThreadPool(chunks);
		var chunkLength = localFile.length() / chunks;
		var finalLength = localFile.length() % chunkLength;
		var progressChunks = Collections.synchronizedList(new ArrayList<FileTransferProgressWrapper>());
		var errors = Collections.synchronizedList(new ArrayList<Throwable>());
		var total = new AtomicLong();
		if (progress.isPresent()) {
			progress.get().started(localFile.length(), localFile.getName());
		}
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
					sendChunk(localFile, pointer, thisLength, chunk, lastChunk, wrapper, preAllocation);
				} catch (Throwable e) {
					errors.add(e);
				}
			});

		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			throw new InterruptedIOException();
		} finally {
			progress.get().completed();
		}

		return errors;
	}

	private void sendFileViaSFTP(AbstractFile localFile, String remotePath) throws IOException, SshException,
			PermissionDeniedException, SftpStatusException, TransferCancelledException {
		var ssh = clients.removeFirst();
		try (var sftp = new SftpClient(ssh)) {
			sftp.cd(remoteFolder);
			if(blocksize > 0) {
				sftp.setBlockSize(blocksize);
			}
			if(outstandingRequests > 0) {
				sftp.setMaxAsyncRequests(outstandingRequests);
			}
			sftp.put(localFile.getAbsolutePath(), remotePath, progress.orElse(null));
		} finally {
			clients.addLast(ssh);
		}
	}

	private void sendFileViaSCP(AbstractFile localFile, String remotePath)
			throws SshException, ChannelOpenException, IOException, PermissionDeniedException {

		var ssh = clients.removeFirst();
		try {
			new ScpClient(ssh).putFile(localFile.getAbsolutePath(), remotePath, false, progress.orElse(null), false);
		} finally {
			clients.addLast(ssh);
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
			boolean lastChunk, FileTransferProgress progress, boolean preAllocated)
			throws IOException, SftpStatusException, SshException, TransferCancelledException, ChannelOpenException,
			PermissionDeniedException {

		verboseMessage("Starting chunk {0} at position {1} with length of {2} bytes",
				chunkNumber, pointer, chunkLength);

		var ssh = clients.removeFirst();
		try (var file = new RandomAccessFile(localFile.getAbsolutePath(), "r")) {
			file.seek(pointer);

			if (forceSFTP || preAllocated) {
				try (var sftp = new SftpClient(ssh)) {
					sftp.cd(remoteFolder);
					if (preAllocated) {
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
					} else {
						sftp.put(new ChunkInputStream(file, chunkLength), localFile.getName() + "_part" + chunkNumber,
								progress);
					}
				}
			} else {
				var scp = new ScpClientIO(ssh);
				scp.put(new ChunkInputStream(file, chunkLength), chunkLength, localFile.getName(),
						FileUtils.checkEndsWithSlash(remoteFolder) + localFile.getName() + "_part" + chunkNumber,
						progress);
			}
		} 
		catch(IOException ioe) {
			if(ioe.getCause() instanceof TransferCancelledException) {
				throw (TransferCancelledException)ioe.getCause();
			}
			else
				throw ioe;
		} finally {
			clients.addLast(ssh);
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
