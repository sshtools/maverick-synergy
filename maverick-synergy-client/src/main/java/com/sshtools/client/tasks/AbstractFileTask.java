package com.sshtools.client.tasks;

import java.util.Optional;

public abstract class AbstractFileTask extends AbstractConnectionTask {
	
	public static abstract class AbstractFileTaskBuilder<B extends AbstractFileTaskBuilder<B, T>, T extends AbstractConnectionTask> extends 
		AbstractConnectionTaskBuilder<B,T> {

		private Optional<FileTransferProgress> progress = Optional.empty();

		
		/**
		 * Set a {@link FileTransferProgress} for the overall progress of the transfer.
		 * {@link FileTransferProgress#started(long, String)} may be called multiple
		 * times if multiple files are being transferred.
		 * 
		 * @param progress overall progress monitor
		 * @return builder for chaining
		 */
		@SuppressWarnings("unchecked")
		public B withProgress(Optional<FileTransferProgress> progress) {
			this.progress = progress;
			return (B) this;
		}
		
		/**
		 * Set a {@link FileTransferProgress} for the overall progress of the transfer.
		 * {@link FileTransferProgress#started(long, String)} may be called multiple
		 * times if multiple files are being transferred.
		 * 
		 * @param progress overall progress monitor
		 * @return builder for chaining
		 */
		public B withProgress(FileTransferProgress progress) {
			return withProgress(Optional.of(progress));
		}
		
		public abstract T build();
	}
	

	protected final Optional<FileTransferProgress> progress;
	
	protected void doTaskUntilDone(Task task) {
		try {
			con.addTask(task).waitForever();
		} finally {
			lastError = task.getLastError();
			done(task.isDone() && task.isSuccess());
		}
	}

	
	public AbstractFileTask(AbstractFileTaskBuilder<?,?> builder) {
		super(builder);
		this.progress = builder.progress;
	}

}
