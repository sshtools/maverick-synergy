package com.sshtools.client;

import java.io.File;

import com.sshtools.client.tasks.DownloadFileTask.DownloadFileTaskBuilder;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;
import com.sshtools.common.logger.Log;

public class Ssh {

	
	
	public static boolean getFile(String host, int port, String username, char[] password, String fromPath, File toFile) {
		
		try (var client = new SshClient(host, port, username, password)) {
			var task = client.addTask(DownloadFileTaskBuilder.create().
					withConnection(client.getConnection()). 
					withRemotePath(fromPath).
					withLocalFile(toFile).build()).waitForever();
			return task.isDone() && task.isSuccess();
		} catch (Throwable e) {
			Log.error("getFile failed", e);
			return false;
		}
	}
	
	public static boolean putFile(String host, int port, String username, char[] password, File fromFile, String toPath) {
		
		try (var client = new SshClient(host, port, username, password)) {
			var task = client.addTask(UploadFileTaskBuilder.create().
				withConnection(client.getConnection()).
				withLocalFile(fromFile).
				withRemotePath(toPath).build()).waitForever();
			return task.isDone() && task.isSuccess();
		} catch (Throwable e) {
			Log.error("putFile failed", e);
			return false;
		}
	}
}
