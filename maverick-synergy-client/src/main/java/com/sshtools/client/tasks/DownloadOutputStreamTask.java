
package com.sshtools.client.tasks;

import java.io.OutputStream;

import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

public class DownloadOutputStreamTask extends Task {

	String path;
	OutputStream localFile = null;
	
	public DownloadOutputStreamTask(SshConnection con, String path, OutputStream localFile) {
		super(con);
		this.path = path;
		this.localFile = localFile;
	}
	
	public DownloadOutputStreamTask(SshConnection con, String path) {
		this(con, path, null);
	}

	@Override
	protected void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					get(path, localFile);
				} catch (SftpStatusException | SshException | TransferCancelledException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
		};
		
		try {
			con.addTask(new ConnectionTaskWrapper(con, task));
			task.waitForever();
		} finally {
			IOUtils.closeStream(localFile);
			done(task.isDone() && task.isSuccess());
		}
	}
}
