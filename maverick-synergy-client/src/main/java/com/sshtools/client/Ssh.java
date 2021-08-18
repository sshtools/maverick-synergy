/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.client;

import java.io.File;

import com.sshtools.client.tasks.DownloadFileTask;
import com.sshtools.client.tasks.UploadFileTask;
import com.sshtools.common.logger.Log;

public class Ssh {

	
	
	public static boolean getFile(String host, int port, String username, char[] password, String fromPath, File toFile) {
		
		try (SshClient client = new SshClient(host, port, username, password)) {
			DownloadFileTask task = new DownloadFileTask(client.getConnection(), 
					fromPath,
					toFile);
			client.addTask(task);
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
			client.addTask(task);
			task.waitForever();
			return task.isDone() && task.isSuccess();
		} catch (Throwable e) {
			Log.error("putFile failed", e);
			return false;
		}
	}
}
