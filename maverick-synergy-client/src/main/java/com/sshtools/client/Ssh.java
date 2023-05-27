/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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
