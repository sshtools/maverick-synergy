
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
