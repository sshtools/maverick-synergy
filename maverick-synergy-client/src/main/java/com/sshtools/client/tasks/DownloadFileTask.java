package com.sshtools.client.tasks;

import java.io.File;
import java.io.FileNotFoundException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.FileSystemUtils;

public class DownloadFileTask extends Task {

	Connection<SshClientContext> con;
	String path;
	File localFile = null;
	Throwable e;
	
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
			protected void doTask() {
				try {
					if(localFile==null) {
						localFile = new File(lpwd(), FileSystemUtils.getFilename(path));
					}
					get(path, localFile.getAbsolutePath());
				} catch (FileNotFoundException | SftpStatusException | SshException | TransferCancelledException e) {
					DownloadFileTask.this.e = e;
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

	@Override
	public Throwable getLastError() {
		return e;
	}
}
