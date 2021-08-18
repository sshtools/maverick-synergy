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

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.synergy.ssh.Connection;

public class DownloadFileTask extends Task {

	Connection<SshClientContext> con;
	String path;
	File localFile = null;
	
	public DownloadFileTask(Connection<SshClientContext> con, String path, File localFile) {
		super(con);
		this.con = con;
		this.path = path;
		this.localFile = localFile;
	}
	
	public DownloadFileTask(Connection<SshClientContext> con, String path) {
		this(con, path, null);
	}

	public void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					if(localFile==null) {
						localFile = new File(lpwd(), FileUtils.getFilename(path));
					}
					get(path, localFile.getAbsolutePath());
				} catch (SftpStatusException | SshException | TransferCancelledException | IOException | PermissionDeniedException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
		};
		
		try {
			con.addTask(task);
			task.waitForever();
		} finally {
			done(task.isDone() && task.isSuccess());
		}
	}

	public File getDownloadedFile() {
		return localFile;
	}

}
