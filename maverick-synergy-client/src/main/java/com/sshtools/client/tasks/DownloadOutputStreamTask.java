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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.tasks;

import java.io.OutputStream;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtil;

public class DownloadOutputStreamTask extends AbstractRequestFuture implements Runnable {

	Connection<SshClientContext> con;
	String path;
	OutputStream localFile = null;
	Throwable e;
	
	public DownloadOutputStreamTask(Connection<SshClientContext> con, String path, OutputStream localFile) {
		this.con = con;
		this.path = path;
		this.localFile = localFile;
	}
	
	public DownloadOutputStreamTask(Connection<SshClientContext> con, String path) {
		this(con, path, null);
	}

	public void run() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					get(path, localFile);
				} catch (SftpStatusException | SshException | TransferCancelledException e) {
					DownloadOutputStreamTask.this.e = e;
				}
			}
		};
		
		try {
			con.addTask(new ConnectionTaskWrapper(con, task));
			task.waitForever();
		} finally {
			IOUtil.closeStream(localFile);
			done(task.isDone() && task.isSuccess());
		}
	}

	public Throwable getError() {
		return e;
	}
}
