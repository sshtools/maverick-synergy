package com.sshtools.client;

import java.io.File;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.client.tasks.DownloadFileTask;
import com.sshtools.client.tasks.UploadFileTask;

public class Ssh {

	
	
	public static boolean getFile(String host, int port, String username, char[] password, String fromPath, File toFile) {
		
		try (SshClient client = new SshClient(host, port, username, password)) {
			DownloadFileTask task = new DownloadFileTask(client.getConnection(), 
					fromPath,
					toFile);
			client.addTask(new ConnectionTaskWrapper(client.getConnection(), task));
			task.waitForever();
			return task.isDone() && task.isSuccess();
		} catch (Throwable e) {
			Log.error("getFile failed", e);
			return false;
		}
	}
	
	public static boolean putFile(String host, int port, String username, char[] password, File fromFile, String toPath) {
		
		try (SshClient client = new SshClient(host, port, username, password)) {
			UploadFileTask task = new UploadFileTask(client.getConnection(), 
					fromFile,
					toPath);
			client.addTask(new ConnectionTaskWrapper(client.getConnection(), task));
			task.waitForever();
			return task.isDone() && task.isSuccess();
		} catch (Throwable e) {
			Log.error("putFile failed", e);
			return false;
		}
	}
}
