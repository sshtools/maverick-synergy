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
