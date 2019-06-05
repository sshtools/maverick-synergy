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
			protected void doTask() {
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
