package com.sshtools.client;

import java.io.File;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.tasks.DownloadFileTask.DownloadFileTaskBuilder;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;
import com.sshtools.common.logger.Log;

public class Ssh {

	public static boolean getFile(String host, int port, String username, char[] password, String fromPath, File toFile) {
		
		try (var client = SshClientBuilder.create().
				withTarget(host, port).
				withUsername(username).
				withPassword(password).build()) {
			
			return client.addTask(DownloadFileTaskBuilder.create().
					withClient(client). 
					withRemotePath(fromPath).
					withLocalFile(toFile).build()).waitForever().isDoneAndSuccess();
		} catch (Throwable e) {
			Log.error("getFile failed", e);
			return false;
		}
	}

	public static boolean putFile(String host, int port, String username, char[] password, File fromFile, String toPath) {
		try (var client = SshClientBuilder.create().
				withTarget(host, port).
				withUsername(username).
				withPassword(password).build()) {
			
			return client.addTask(UploadFileTaskBuilder.create().
				withClient(client).
				withLocalFile(fromFile).
				withRemotePath(toPath).build()).waitForever().isDoneAndSuccess();
			
		} catch (Throwable e) {
			Log.error("putFile failed", e);
			return false;
		}
	}
}
