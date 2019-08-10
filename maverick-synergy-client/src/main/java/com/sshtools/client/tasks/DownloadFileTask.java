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

import java.io.File;
import java.io.FileNotFoundException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileSystemUtils;

public class DownloadFileTask extends Task {

	Connection<SshClientContext> con;
	String path;
	File localFile = null;
	Throwable e;
	
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
						localFile = new File(lpwd(), FileSystemUtils.getFilename(path));
					}
					get(path, localFile.getAbsolutePath());
				} catch (FileNotFoundException | SftpStatusException | SshException | TransferCancelledException e) {
					DownloadFileTask.this.e = e;
				}
			}

			@Override
			public Throwable getLastError() {
				// TODO Auto-generated method stub
				return null;
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

	@Override
	public Throwable getLastError() {
		return e;
	}
}
