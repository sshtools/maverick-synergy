/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
