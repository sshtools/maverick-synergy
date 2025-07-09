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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.sshtools.client.ChunkInputStream;
import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpHandle;
import com.sshtools.client.sftp.SftpMessage;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
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
 * client.addTask(PushTaskBuilder.create().
 * 		withFilePaths("a/b/c.txt", "/d/e/f/g.txt").
 * 		withRemoteFolder("/path/on/remote").
 * 		withChunks(5).
 * 		build());
 * </pre>
 *
 */
public final class PushTask extends AbstractOptimisedTask<String, AbstractFile> {
	/**
	 * Builder for {@link PushTask}.
	 */
	public static class PushTaskBuilder extends AbstractOptimisedTaskBuilder<PushTaskBuilder, PushTask, AbstractFile> {

		private Optional<Path> remoteFolder = Optional.empty();
		private List<Path> paths = new ArrayList<>();
		private List<AbstractFile> files = new ArrayList<>();
		
		private PushTaskBuilder() {
			super();
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
		 * Add a collection of AbstractFile objects to transfer. Each should be the path of the
		 * <code>Local</code> file, and will be resolved against the current virtual
		 * file system configured on the {@link SftpClient}.
		 * 
		 * @param filePaths file paths to add.
		 * @return builder for chaining
		 */
		public PushTaskBuilder addAbstactFiles(Collection<AbstractFile> filePaths) {
			this.files.addAll(filePaths);
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
		 * Set a collection of file paths to transfer. Any paths already added to this
		 * builder will be replaced. Each should be the path of the <code>Local</code>
		 * file, and will be resolved against the current virtual file system configured
		 * on the {@link SftpClient}.
		 * 
		 * @param paths all file paths to transfer.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withAbstractFiles(Collection<AbstractFile> paths) {
			this.files.clear();
			return addAbstactFiles(paths);
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
		 * @param paths all file paths to transfer.
		 * @return builder for chaining
		 */
		public PushTaskBuilder withAbstractFiles(AbstractFile... paths) {
			return withAbstractFiles(Arrays.asList(paths));
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

	private final List<AbstractFile> files;
	private final String remoteFolder;

	PushTask(PushTaskBuilder builder) {
		super(builder);
		this.remoteFolder = builder.remoteFolder.map(Utils::translatePathString).orElse(null);
		this.files = new ArrayList<AbstractFile>();
		this.files.addAll(builder.files);
		
		for(var file : Collections.unmodifiableList(new ArrayList<Path>(builder.paths))) {
			try {
				var resolved = primarySftpClient.getCurrentWorkingDirectory().resolveFile(file.toString());
				if(!resolved.exists()) {
					throw new FileNotFoundException(String.format("%s does not exist", file.getFileName()));
				}
				this.files.add(resolved);
			} catch (IOException | PermissionDeniedException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void transferFiles(String targetFolder) throws SftpStatusException, SshException, TransferCancelledException, IOException,
			PermissionDeniedException, ChannelOpenException {

		var remoteAttrs = primarySftpClient.stat(targetFolder);
		if (!remoteAttrs.isDirectory()) {
			throw new IOException("Remote directory must be a directory!");
		}

		verboseMessage("The paths will be transferred to {0}", targetFolder);

		for (var file : files) {
			transferFile(file, targetFolder);
		}
	}

	@Override
	protected String configureTargetFolder()
			throws IOException, SshException, PermissionDeniedException, SftpStatusException {

		String target;
		if (Utils.isNotBlank(remoteFolder)) {
			target = primarySftpClient.getAbsolutePath(remoteFolder);
		} else {
			target = primarySftpClient.getAbsolutePath(".");
		}
		return target;
	}

	private void transferFile(AbstractFile localFile, String remoteFolder) throws SftpStatusException, SshException, TransferCancelledException,
			IOException, PermissionDeniedException, ChannelOpenException {

		
		verboseMessage("Total to transfer is {0} bytes", localFile.length());

		if (chunks <= 1) {
			sendFileViaSFTP(localFile, "", remoteFolder);
		} else {
			checkErrors(sendChunks(localFile, remoteFolder));
		}
		
		verifyIntegrity(Paths.get(localFile.getAbsolutePath()), remoteFolder + "/" + localFile.getName());
	}

	private Collection<Throwable> sendChunks(AbstractFile localFile, String remoteFolder)
			throws PermissionDeniedException, IOException, SftpStatusException, SshException {

		var executor = Executors.newFixedThreadPool(chunks);
		
		try {
			
			var targetFilePath = remoteFolder + "/" + localFile.getName();
			if(!primarySftpClient.exists(targetFilePath)) {
				verboseMessage("Pre-creating file {0}/{1}", remoteFolder, localFile.getName(), chunks);
				primarySftpClient.openFile(targetFilePath, SftpChannel.OPEN_WRITE | SftpChannel.OPEN_CREATE).close();
			}

			if (progress.isPresent()) {
				progress.get().started(localFile.length(), localFile.getName());
			}
			
			String remotePath = FileUtils.checkEndsWithSlash(primarySftpClient.pwd()) + localFile.getName();

			ByteArrayWriter msg = new ByteArrayWriter();
			msg.writeString(remotePath);
		
			UnsignedInteger32 requestId;
			
			var progressChunks = Collections.synchronizedList(new ArrayList<FileTransferProgressWrapper>());
			var errors = Collections.synchronizedList(new ArrayList<Throwable>());
			var total = new AtomicLong();
			
			try {
				
				requestId = primarySftpClient.getSubsystemChannel().sendExtensionMessage("create-multipart-file@sshtools.com", msg.toByteArray());

				SftpMessage ext = primarySftpClient.getSubsystemChannel().getExtendedReply(requestId, remotePath);
				byte[] handle = ext.readBinaryString();
				var blocksize = ext.readInt();
				
				@SuppressWarnings("unused")
				int method = ext.read();

				SftpHandle transaction = primarySftpClient.getSubsystemChannel().createHandle(handle, remotePath);
				
				verboseMessage("Remote server supports multipart extensions with minimum part size of {0} bytes", blocksize);
				
				if(localFile.length() <= blocksize) {
					verboseMessage("Minimum blocksize for push not met reverting to put");
					try {
						sendFileViaSFTP(localFile, remotePath, remoteFolder);
					} catch (TransferCancelledException e) {
						/**
						 * LDP - Is this the correct behaviour?
						 */
						return errors;
					}
				} else {
				
					var totalBlocks = localFile.length() / blocksize;
					if(localFile.length() % blocksize > 0) totalBlocks++;
					
					var blocksPerChunk = (totalBlocks / chunks);
					var chunkLength = blocksPerChunk * blocksize;
					var finalLength = localFile.length() - ((chunks - 1) * chunkLength);
					
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
								var thisLength = lastChunk ? finalLength : chunkLength;
								
								sendPart(localFile, pointer, thisLength, chunk, lastChunk, wrapper, transaction, String.format("part%d", chunk), remoteFolder);
								
							} catch (Throwable e) {
								errors.add(e);
							}
						});
						
					}
				}
				
			} catch(SftpStatusException e) {
				/**
				 * Fallback to standard random access mode. Don't be so strict on the reason
				 * code because we don't know what other servers are sending back in response
				 * to an unknown extension.
				 */
				var chunkLength = localFile.length() / chunks;
				var finalLength = localFile.length() - ((chunks - 1) * chunkLength);
				
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
							var thisLength = lastChunk ? finalLength : chunkLength;
							sendChunk(localFile, pointer, thisLength, chunk, lastChunk, wrapper, remoteFolder);
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

	private void sendFileViaSFTP(AbstractFile localFile, String remotePath, String remoteFolder) throws IOException, SshException,
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

	private void sendChunk(AbstractFile localFile, long pointer, long chunkLength, Integer chunkNumber,
			boolean lastChunk, FileTransferProgress progress, String remoteFolder)
			throws IOException, SftpStatusException, SshException, TransferCancelledException, ChannelOpenException,
			PermissionDeniedException {

		SshClient ssh;
		synchronized (clients) {
			ssh = clients.removeFirst();
		}
		try (var file = localFile.openFile(false)) {
			file.seek(pointer);
			try (var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withBlockSize(blocksize).
					withAsyncRequests(outstandingRequests).
					withRemotePath(remoteFolder).
					withLocalPath(primarySftpClient.lpwd()).
					build()) {

				try {
					sftp.put(new ChunkInputStream(file, chunkLength), localFile.getName(), new FileTransferProgress() {

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
				} catch (SftpStatusException e) {
					if (e.getStatus() == SftpStatusException.SSH_FX_NO_SUCH_FILE) {
						FileNotFoundException fnfe = new FileNotFoundException(localFile.getName() + " (chunk "
								+ chunkNumber + " @ " + pointer + ", with " + chunkLength + " bytes)");
						fnfe.initCause(e);
						throw fnfe;
					} else
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
	
	private void sendPart(AbstractFile localFile, long pointer, long chunkLength, Integer chunkNumber,
			boolean lastChunk, FileTransferProgress progress, SftpHandle transaction, String partId, String remoteFolder)
			throws IOException, SftpStatusException, SshException, TransferCancelledException, ChannelOpenException,
			PermissionDeniedException {

		SshClient ssh;
		synchronized(clients) {
			ssh = clients.removeFirst();
		}
		try (var file = localFile.openFile(false)) {
			
			file.seek(pointer);
			try (var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath(remoteFolder).
					withLocalPath(primarySftpClient.lpwd()).
					build()) {

				var msg = new ByteArrayWriter();
				msg.writeBinaryString(transaction.getHandle());
				msg.writeString(partId);
				msg.writeUINT64(pointer);
				msg.writeUINT64(chunkLength);
				
				try(var handle = sftp.getSubsystemChannel().getHandle(sftp.getSubsystemChannel().sendExtensionMessage("open-part-file@sshtools.com", msg.toByteArray()), transaction.getFile())) {
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
}
