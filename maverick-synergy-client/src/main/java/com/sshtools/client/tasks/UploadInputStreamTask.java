
package com.sshtools.client.tasks;

import java.io.InputStream;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

public class UploadInputStreamTask extends Task {

	String path;
	InputStream in = null;
	
	public UploadInputStreamTask(SshClient ssh, InputStream in, String path) {
		this(ssh.getConnection(), in, path);
	}
	
	public UploadInputStreamTask(SshConnection con, InputStream in, String path) {
		super(con);
		this.path = path;
		this.in = in;
	}

	protected void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					put(in, path);
				} catch (SftpStatusException | SshException | TransferCancelledException e) {
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
