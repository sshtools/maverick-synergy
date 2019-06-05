package com.sshtools.client.tasks;

import java.io.InputStream;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.SshException;

public class UploadInputStreamTask extends AbstractRequestFuture implements Runnable {

	Connection<SshClientContext> con;
	String path;
	InputStream in = null;
	Throwable e;

	
	public UploadInputStreamTask(Connection<SshClientContext> con, InputStream in, String path) {
		this.con = con;
		this.path = path;
		this.in = in;
	}

	public void run() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doTask() {
				try {
					put(in, path);
				} catch (SftpStatusException | SshException | TransferCancelledException e) {
					UploadInputStreamTask.this.e = e;
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

	public Throwable getError() {
		return e;
	}

}
