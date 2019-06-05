package com.sshtools.client.tasks;

import java.io.File;
import java.io.FileNotFoundException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.SshException;

public class UploadFileTask extends Task {

	Connection<SshClientContext> con;
	String path;
	File localFile = null;
	Exception e;
	
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
			protected void doTask() {
				try {
					if(path==null) {
						put(localFile.getAbsolutePath());
					} else {
						put(localFile.getAbsolutePath(), path);
					}
				} catch (FileNotFoundException | SftpStatusException | SshException | TransferCancelledException e) {
					UploadFileTask.this.e = e;
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

	@Override
	public Throwable getLastError() {
		return e;
	}

}
