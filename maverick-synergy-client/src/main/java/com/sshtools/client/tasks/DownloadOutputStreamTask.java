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

import java.io.OutputStream;

import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

public class DownloadOutputStreamTask extends Task {

	String path;
	OutputStream localFile = null;
	
	public DownloadOutputStreamTask(SshConnection con, String path, OutputStream localFile) {
		super(con);
		this.path = path;
		this.localFile = localFile;
	}
	
	public DownloadOutputStreamTask(SshConnection con, String path) {
		this(con, path, null);
	}

	@Override
	protected void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					get(path, localFile);
				} catch (SftpStatusException | SshException | TransferCancelledException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
		};
		
		try {
			con.addTask(new ConnectionTaskWrapper(con, task));
			task.waitForever();
		} finally {
			IOUtils.closeStream(localFile);
			done(task.isDone() && task.isSuccess());
		}
	}
}
