
package com.sshtools.client.tasks;

import java.io.File;
import java.io.IOException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

public class UploadFileTask extends Task {

	Connection<SshClientContext> con;
	String path;
	File localFile = null;
	
	public UploadFileTask(Connection<SshClientContext> con, File localFile, String path) {
		super(con);
		this.con = con;
		this.path = path;
		this.localFile = localFile;
	}
	
	public UploadFileTask(Connection<SshClientContext> con, File localFile) {
		this(con, localFile, null);
	}

	public void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					if(path==null) {
						put(localFile.getAbsolutePath());
					} else {
						put(localFile.getAbsolutePath(), path);
					}
				} catch (SftpStatusException | SshException | TransferCancelledException | IOException | PermissionDeniedException e) {
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
