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
