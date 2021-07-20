
package com.sshtools.client.tasks;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.ssh.Connection;

public class StatTask extends Task {

	Connection<SshClientContext> con;
	String path;
	SftpFileAttributes attrs = null;
	
	public StatTask(Connection<SshClientContext> con, String path) {
		super(con);
		this.con = con;
		this.path = path;
	}

	public void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {

					attrs = stat(path);
				} catch (SftpStatusException | SshException e) {
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

	public SftpFileAttributes getAttributes() {
		return attrs;
	}

}
