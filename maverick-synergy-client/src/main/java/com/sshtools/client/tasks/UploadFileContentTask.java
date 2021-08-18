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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

public class UploadFileContentTask extends Task {

	Connection<SshClientContext> con;
	String path;
	String content;
	String encoding;
	
	public UploadFileContentTask(SshClient ssh, String content, String encoding, String path) {
		this(ssh.getConnection(), content, encoding, path);
	}
	
	public UploadFileContentTask(Connection<SshClientContext> con, String content, String encoding, String path) {
		super(con);
		this.con = con;
		this.path = path;
		this.encoding = encoding;
		this.content = content;
	}
	
	public void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					put(new ByteArrayInputStream(content.getBytes(encoding)), path);
				} catch (SftpStatusException | SshException | TransferCancelledException | UnsupportedEncodingException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
		};
		
		try {
			con.addTask(new ConnectionTaskWrapper(con, task));
			task.waitForever();
		} finally {
			done(task.isDone() && task.isSuccess());
		}
	}
}
