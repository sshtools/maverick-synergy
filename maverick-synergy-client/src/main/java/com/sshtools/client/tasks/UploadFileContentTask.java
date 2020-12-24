/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.common.ssh.Connection;
import com.sshtools.synergy.common.ssh.ConnectionTaskWrapper;

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
