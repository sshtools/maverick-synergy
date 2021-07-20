
package com.sshtools.client.tasks;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;

public class UploadFileContentTask extends Task {

	Connection<SshClientContext> con;
	String path;
	String content;
	String encoding;
	
	public UploadFileContentTask(SshClient ssh, String content, String encoding, String path) {
		this(ssh.getConnection(), content, encoding, path);
	}
	
	public UploadFileContentTask(Connection<SshClientContext> con, String content, String encoding, String path) {
		super(con);
		this.con = con;
		this.path = path;
		this.encoding = encoding;
		this.content = content;
	}
	
	public void doTask() {
		
		SftpClientTask task = new SftpClientTask(con) {
			
			@Override
			protected void doSftp() {
				try {
					put(new ByteArrayInputStream(content.getBytes(encoding)), path);
				} catch (SftpStatusException | SshException | TransferCancelledException | UnsupportedEncodingException e) {
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
